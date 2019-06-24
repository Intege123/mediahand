package mediahand.repository;

import mediahand.WatchState;
import mediahand.domain.DirectoryEntry;
import mediahand.domain.MediaEntry;
import mediahand.repository.base.BaseRepository;
import mediahand.repository.base.Database;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

import static utils.Nullcheck.checkNotNull;

public class MediaRepository implements BaseRepository<MediaEntry> {

    @Override
    public MediaEntry create(MediaEntry entry) {
        checkNotNull(entry, "entry");

        try {
            Database.getStatement().execute("INSERT INTO mediaTable (Title, Episodes, MediaType, WatchState, Path, EpisodeLength, DIRTABLE_FK) " +
                    "VALUES('" + entry.getTitle() + "', " + entry.getEpisodeNumber() + ", '" + entry.getMediaType() + "', '" + entry.getWatchState().getValue() + "', '" +
                    entry.getPath() + "', " + entry.getEpisodeLength() + ", " + entry.getBasePath().getId() + ")");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("The entry '" + entry.getTitle() + "' already exists.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return find(entry);
    }

    @Override
    public MediaEntry update(MediaEntry entry) {
        checkNotNull(entry, "entry");

        try {
            Database.getStatement().execute("UPDATE MEDIATABLE SET TITLE = '" + entry.getTitle() + "', EPISODES = '" +
                    entry.getEpisodeNumber() + "', MEDIATYPE = '" + entry.getMediaType() + "', WATCHSTATE = '" + entry.getWatchState().getValue() + "', CURRENTEPISODE = '" +
                    entry.getCurrentEpisode() + "' WHERE ID = '" + entry.getId() + "'");
        } catch (SQLException e) {
            System.err.println("Could not update media entry: " + entry.getTitle());
            e.printStackTrace();
        }
        return find(entry);
    }

    @Override
    public void remove(MediaEntry entry) {
        checkNotNull(entry, "entry");
        try {
            Database.getStatement().execute("DELETE FROM mediaTable WHERE Title = '" + entry.getTitle() + "'");
        } catch (SQLException e) {
            System.err.println("Could not remove media entry: " + entry.getTitle());
            e.printStackTrace();
        }
    }

    @Override
    public MediaEntry find(MediaEntry entry) {
        checkNotNull(entry, "entry");
        try {
            ResultSet result = Database.getStatement().executeQuery("SELECT MEDIATABLE.ID, TITLE, EPISODES, MEDIATYPE, WATCHSTATE, " +
                    "RATING, MEDIATABLE.PATH, CURRENTEPISODE, ADDED, EPISODELENGTH, WATCHEDDATE, WATCHNUMBER, " +
                    "DIRTABLE_FK, DIRTABLE.ID AS dirtable_id, DIRTABLE.PATH AS dirtable_path FROM MEDIATABLE, DIRTABLE " +
                    "WHERE MEDIATABLE.DIRTABLE_FK = DIRTABLE.ID AND TITLE = '" + entry.getTitle() + "'");
            if (result.next()) {
                String dirtable_path = result.getString("dirtable_path");
                if (new File(dirtable_path).exists()) {
                    return new MediaEntry(result.getInt("ID"), result.getString("TITLE"), result.getInt("EPISODES"),
                            result.getString("MEDIATYPE"), WatchState.valueOf(result.getString("WATCHSTATE")),
                            result.getInt("RATING"), result.getString("PATH"), result.getInt("CURRENTEPISODE"),
                            result.getDate("ADDED"), result.getInt("EPISODELENGTH"), result.getDate("WATCHEDDATE"),
                            result.getInt("WATCHNUMBER"), new DirectoryEntry(result.getInt("dirtable_id"), dirtable_path));
                } else {
                    System.err.println("\"" + dirtable_path + "\" does not exist, is not a directory or is currently not connected.");
                }
            } else {
                System.err.println("No entry \"" + entry.getTitle() + "\" found.");
            }
        } catch (SQLException e) {
            System.err.println("Could not find media entry: " + entry.getTitle());
            e.printStackTrace();
        }
        return entry;
    }

    @Override
    public List<MediaEntry> findAll() {
        List<MediaEntry> mediaEntries = new ArrayList<>();

        try {
            ResultSet result = Database.getStatement().executeQuery("SELECT MEDIATABLE.ID, TITLE, EPISODES, MEDIATYPE, WATCHSTATE, " +
                    "RATING, MEDIATABLE.PATH, CURRENTEPISODE, ADDED, EPISODELENGTH, WATCHEDDATE, WATCHNUMBER, " +
                    "DIRTABLE_FK, DIRTABLE.ID AS dirtable_id, DIRTABLE.PATH AS dirtable_path FROM MEDIATABLE, DIRTABLE WHERE MEDIATABLE.DIRTABLE_FK = DIRTABLE.ID");
            while (result.next()) {
                String dirtable_path = result.getString("dirtable_path");
                if (new File(dirtable_path).exists()) {
                    String watchstate = result.getString("WATCHSTATE");
                    mediaEntries.add(new MediaEntry(result.getInt("ID"), result.getString("TITLE"), result.getInt("EPISODES"),
                            result.getString("MEDIATYPE"), WatchState.valueOf(watchstate),
                            result.getInt("RATING"), result.getString("PATH"), result.getInt("CURRENTEPISODE"),
                            result.getDate("ADDED"), result.getInt("EPISODELENGTH"), result.getDate("WATCHEDDATE"),
                            result.getInt("WATCHNUMBER"), new DirectoryEntry(result.getInt("dirtable_id"), dirtable_path)));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mediaEntries;
    }
}
