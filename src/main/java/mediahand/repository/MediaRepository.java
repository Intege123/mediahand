package mediahand.repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mediahand.WatchState;
import mediahand.controller.MediaHandAppController;
import mediahand.domain.DirectoryEntry;
import mediahand.domain.MediaEntry;
import mediahand.repository.base.BaseRepository;
import mediahand.repository.base.Database;
import mediahand.utils.MessageUtil;
import utils.Check;

public class MediaRepository implements BaseRepository<MediaEntry> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaRepository.class);

    @Override
    public MediaEntry create(MediaEntry entry) {
        Check.notNullArgument(entry, "entry");

        try {
            Database.getStatement().execute(
                    "INSERT INTO mediaTable (Title, Episodes, MediaType, WatchState, Path, EpisodeLength, Volume, DIRTABLE_FK) "
                            +
                            "VALUES('" + entry.getTitle() + "', " + entry.getEpisodeNumber() + ", '"
                            + entry.getMediaType() + "', '" + entry.getWatchState() + "', '" +
                            entry.getPath() + "', " + entry.getEpisodeLength() + ", " + entry.getVolume() + ", "
                            + entry.getBasePathId() + ")");
        } catch (SQLException e) {
            LOGGER.error("create", e);
        }
        return find(entry);
    }

    @Override
    public MediaEntry update(MediaEntry entry) {
        Check.notNullArgument(entry, "entry");

        String watchedDate = null;
        if (entry.getWatchedDate() != null) {
            watchedDate = "'" + entry.getWatchedDate() + "'";
        }
        try {
            Database.getStatement().execute("UPDATE MEDIATABLE SET TITLE = '" + entry.getTitle() + "', EPISODES = '" +
                    entry.getEpisodeNumber() + "', MEDIATYPE = '" + entry.getMediaType() + "', WATCHSTATE = '"
                    + entry.getWatchState() + "', CURRENTEPISODE = '" +
                    entry.getCurrentEpisodeNumber() + "', Volume='" + entry.getVolume() + "', DIRTABLE_FK = "
                    + entry.getBasePathId() + ", PATH = '" + entry.getPath() +
                    "', Rating=" + entry.getRating() + ", WatchNumber=" + entry.getWatchedCount() + ", WatchedDate="
                    + watchedDate + ", SUBTITLETRACK=" + entry.getSubtitleTrack() + ", AUDIOTRACK="
                    + entry.getAudioTrack() + " WHERE ID = '"
                    + entry.getId() + "'");
            MediaHandAppController.triggerMediaEntryUpdate(entry);
        } catch (SQLException e) {
            LOGGER.error("Could not update media entry: {} - {}", entry.getTitle(), e.getMessage());
        }
        return find(entry);
    }

    @Override
    public void remove(MediaEntry entry) {
        Check.notNullArgument(entry, "entry");
        try {
            Database.getStatement().execute("DELETE FROM mediaTable WHERE Title = '" + entry.getTitle() + "'");
            MediaHandAppController.getMediaEntries().remove(entry);
        } catch (SQLException e) {
            LOGGER.error("Could not remove media entry: {}, {}", entry.getTitle(), e.getMessage());
        }
    }

    @Override
    public MediaEntry find(MediaEntry entry) {
        Check.notNullArgument(entry, "entry");
        try (ResultSet result = Database.getStatement().executeQuery(
                "SELECT MEDIATABLE.ID, TITLE, EPISODES, MEDIATYPE, WATCHSTATE, "
                        + "RATING, MEDIATABLE.PATH, CURRENTEPISODE, ADDED, EPISODELENGTH, WATCHEDDATE, WATCHNUMBER, VOLUME, AUDIOTRACK, SUBTITLETRACK, "
                        + "DIRTABLE_FK, DIRTABLE.ID AS dirtable_id, DIRTABLE.PATH AS dirtable_path FROM MEDIATABLE, DIRTABLE "
                        + "WHERE TITLE = '" + entry.getTitle() + "'")) {
            if (result.next()) {
                String dirtable_path = result.getString("dirtable_path");
                DirectoryEntry directoryEntry = null;
                if (dirtable_path != null) {
                    directoryEntry = new DirectoryEntry(result.getInt("dirtable_id"), dirtable_path);
                }
                Date watchedDate = result.getDate("WATCHEDDATE");
                LocalDate localWatchedDate = null;
                if (watchedDate != null) {
                    localWatchedDate = watchedDate.toLocalDate();
                }
                return new MediaEntry(result.getInt("ID"), result.getString("TITLE"), result.getInt("EPISODES"),
                        result.getString("MEDIATYPE"), WatchState.lookupByName(result.getString("WATCHSTATE")),
                        result.getInt("RATING"), result.getString("PATH"), result.getInt("CURRENTEPISODE"),
                        result.getDate("ADDED").toLocalDate(), result.getInt("EPISODELENGTH"), localWatchedDate,
                        result.getInt("WATCHNUMBER"), directoryEntry, result.getInt("VOLUME"), result.getString("AUDIOTRACK"), result.getString("SUBTITLETRACK"));
            } else {
                MessageUtil.infoAlert("Find media", "No media entry found: " + entry.getTitle());
            }
        } catch (SQLException e) {
            LOGGER.error("Find media entry: {} - {}", entry.getTitle(), e.getMessage());
        }
        return null;
    }

    @Override
    public List<MediaEntry> findAll() {
        List<MediaEntry> mediaEntries = new ArrayList<>();

        try (ResultSet result = Database.getStatement().executeQuery(
                "SELECT MEDIATABLE.ID, TITLE, EPISODES, MEDIATYPE, WATCHSTATE, "
                        + "RATING, MEDIATABLE.PATH, CURRENTEPISODE, ADDED, EPISODELENGTH, WATCHEDDATE, WATCHNUMBER, VOLUME, AUDIOTRACK, SUBTITLETRACK, "
                        + "DIRTABLE_FK, DIRTABLE.ID AS dirtable_id, DIRTABLE.PATH AS dirtable_path FROM MEDIATABLE LEFT JOIN DIRTABLE ON MEDIATABLE.DIRTABLE_FK = DIRTABLE.ID")) {
            while (result.next()) {
                String dirtable_path = result.getString("dirtable_path");
                String watchstate = result.getString("WATCHSTATE");
                DirectoryEntry directoryEntry = null;
                if (dirtable_path != null) {
                    directoryEntry = new DirectoryEntry(result.getInt("dirtable_id"), dirtable_path);
                }
                Date watchedDate = result.getDate("WATCHEDDATE");
                LocalDate localWatchedDate = null;
                if (watchedDate != null) {
                    localWatchedDate = watchedDate.toLocalDate();
                }
                mediaEntries.add(new MediaEntry(result.getInt("ID"), result.getString("TITLE"), result.getInt("EPISODES"),
                        result.getString("MEDIATYPE"), WatchState.lookupByName(watchstate),
                        result.getInt("RATING"), result.getString("PATH"), result.getInt("CURRENTEPISODE"),
                        result.getDate("ADDED").toLocalDate(), result.getInt("EPISODELENGTH"), localWatchedDate,
                        result.getInt("WATCHNUMBER"), directoryEntry, result.getInt("VOLUME"), result.getString("AUDIOTRACK"), result.getString("SUBTITLETRACK")));
            }
        } catch (SQLException e) {
            LOGGER.error("findAll", e);
        }
        return mediaEntries;
    }
}
