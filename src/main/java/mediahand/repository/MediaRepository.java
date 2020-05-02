package mediahand.repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import mediahand.WatchState;
import mediahand.controller.MediaHandAppController;
import mediahand.domain.DirectoryEntry;
import mediahand.domain.MediaEntry;
import mediahand.repository.base.BaseRepository;
import mediahand.repository.base.Database;
import mediahand.utils.MessageUtil;
import utils.Check;

public class MediaRepository implements BaseRepository<MediaEntry> {

    @Override
    public MediaEntry create(MediaEntry entry) throws SQLException {
        Check.notNullArgument(entry, "entry");

        try {
            Database.getStatement().execute(
                    "INSERT INTO mediaTable (Title, Episodes, MediaType, WatchState, Path, EpisodeLength, Volume, DIRTABLE_FK) "
                            +
                            "VALUES('" + entry.getTitle() + "', " + entry.getEpisodeNumber() + ", '"
                            + entry.getMediaType() + "', '" + entry.getWatchState() + "', '" +
                            entry.getPath() + "', " + entry.getEpisodeLength() + ", " + entry.getVolume() + ", "
                            + entry.getBasePathId() + ")");
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new SQLException("The entry '" + entry.getTitle() + "' already exists.");
        }
        return find(entry);
    }

    @Override
    public MediaEntry update(MediaEntry entry) throws SQLException {
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
                    + watchedDate + " WHERE ID = '"
                    + entry.getId() + "'");
            MediaHandAppController.triggerMediaEntryUpdate(entry);
        } catch (SQLException e) {
            throw new SQLException("Could not update media entry: " + entry.getTitle(), e);
        }
        return find(entry);
    }

    @Override
    public void remove(MediaEntry entry) throws SQLException {
        Check.notNullArgument(entry, "entry");
        try {
            Database.getStatement().execute("DELETE FROM mediaTable WHERE Title = '" + entry.getTitle() + "'");
            MediaHandAppController.getMediaEntries().remove(entry);
        } catch (SQLException e) {
            throw new SQLException("Could not remove media entry: " + entry.getTitle(), e);
        }
    }

    @Override
    public MediaEntry find(MediaEntry entry) throws SQLException {
        Check.notNullArgument(entry, "entry");
        try {
            ResultSet result = Database.getStatement().executeQuery(
                    "SELECT MEDIATABLE.ID, TITLE, EPISODES, MEDIATYPE, WATCHSTATE, " +
                            "RATING, MEDIATABLE.PATH, CURRENTEPISODE, ADDED, EPISODELENGTH, WATCHEDDATE, WATCHNUMBER, VOLUME, "
                            +
                            "DIRTABLE_FK, DIRTABLE.ID AS dirtable_id, DIRTABLE.PATH AS dirtable_path FROM MEDIATABLE, DIRTABLE "
                            +
                            "WHERE TITLE = '" + entry.getTitle() + "'");
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
                        result.getString("MEDIATYPE"), WatchState.valueOf(result.getString("WATCHSTATE")),
                        result.getInt("RATING"), result.getString("PATH"), result.getInt("CURRENTEPISODE"),
                        result.getDate("ADDED").toLocalDate(), result.getInt("EPISODELENGTH"), localWatchedDate,
                        result.getInt("WATCHNUMBER"), directoryEntry, result.getInt("VOLUME"));
            } else {
                MessageUtil.infoAlert("Find media", "No media entry found: " + entry.getTitle());
            }
        } catch (SQLException e) {
            throw new SQLException("Could not find media entry: " + entry.getTitle(), e);
        }
        return entry;
    }

    @Override
    public List<MediaEntry> findAll() throws SQLException {
        List<MediaEntry> mediaEntries = new ArrayList<>();

        ResultSet result = Database.getStatement().executeQuery(
                "SELECT MEDIATABLE.ID, TITLE, EPISODES, MEDIATYPE, WATCHSTATE, " +
                        "RATING, MEDIATABLE.PATH, CURRENTEPISODE, ADDED, EPISODELENGTH, WATCHEDDATE, WATCHNUMBER, VOLUME, "
                        +
                        "DIRTABLE_FK, DIRTABLE.ID AS dirtable_id, DIRTABLE.PATH AS dirtable_path FROM MEDIATABLE LEFT JOIN DIRTABLE ON MEDIATABLE.DIRTABLE_FK = DIRTABLE.ID");
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
                    result.getString("MEDIATYPE"), WatchState.valueOf(watchstate),
                    result.getInt("RATING"), result.getString("PATH"), result.getInt("CURRENTEPISODE"),
                    result.getDate("ADDED").toLocalDate(), result.getInt("EPISODELENGTH"), localWatchedDate,
                    result.getInt("WATCHNUMBER"), directoryEntry, result.getInt("VOLUME")));
        }
        return mediaEntries;
    }
}
