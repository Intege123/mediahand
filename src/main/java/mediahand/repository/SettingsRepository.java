package mediahand.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mediahand.WatchState;
import mediahand.domain.SettingsEntry;
import mediahand.repository.base.BaseRepository;
import mediahand.repository.base.Database;
import mediahand.utils.MessageUtil;
import utils.Check;

public class SettingsRepository implements BaseRepository<SettingsEntry> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsRepository.class);

    @Override
    public SettingsEntry create(SettingsEntry entry) {
        Check.notNullArgument(entry, "entry");
        try {
            SettingsEntry settingsEntry = find(entry);
            if (settingsEntry == null) {
                Database.getStatement().execute(
                        "INSERT INTO SETTINGSTABLE (PROFILE, WIDTH, HEIGHT, AUTOCONTINUE, SHOWALL, WATCHSTATE) VALUES('"
                                + entry.getProfile() + "', '" + entry.getWindowWidth() + "', '"
                                + entry.getWindowHeight() + "', " + entry.isAutoContinue() + ", " + entry.isShowAll()
                                + ", '" + entry.getWatchState() + "')");
                return find(entry);
            } else {
                return settingsEntry;
            }
        } catch (SQLException e) {
            LOGGER.error("create", e);
        }
        return null;
    }

    @Override
    public SettingsEntry update(SettingsEntry entry) {
        Check.notNullArgument(entry, "entry");

        try {
            Database.getStatement().execute(
                    "UPDATE SETTINGSTABLE SET PROFILE = '" + entry.getProfile() + "', WIDTH = '" +
                            entry.getWindowWidth() + "', HEIGHT = '" + entry.getWindowHeight() + "', AUTOCONTINUE = "
                            + entry.isAutoContinue() + ", SHOWALL = " + entry.isShowAll() + ", WATCHSTATE = '"
                            + entry.getWatchState() + "' WHERE ID = '" + entry.getId() + "'");
        } catch (SQLException e) {
            MessageUtil.warningAlert(e, "Could not update settings profile: " + entry.getProfile());
        }
        return find(entry);
    }

    @Override
    public void remove(SettingsEntry entry) {
    }

    @Override
    public SettingsEntry find(SettingsEntry entry) {
        Check.notNullArgument(entry, "entry");
        try (ResultSet result = Database.getStatement().executeQuery(
                "SELECT * FROM SETTINGSTABLE WHERE PROFILE='" + entry.getProfile() + "'")) {
            if (result.next()) {
                return new SettingsEntry(result.getInt("ID"), result.getString("PROFILE"), result.getInt("WIDTH"), result.getInt("HEIGHT"), result.getBoolean("AUTOCONTINUE"), result.getBoolean("SHOWALL"), WatchState.lookupByName(result.getString("WATCHSTATE")));
            }
        } catch (SQLException e) {
            LOGGER.error("find", e);
        }
        return null;
    }

    public SettingsEntry find(final String profile) {
        return find(new SettingsEntry(profile, 0, 0, false, false, null));
    }

    @Override
    public List<SettingsEntry> findAll() {
        return null;
    }

}
