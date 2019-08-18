package mediahand.repository;

import mediahand.domain.SettingsEntry;
import mediahand.repository.base.BaseRepository;
import mediahand.repository.base.Database;
import utils.Check;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SettingsRepository implements BaseRepository<SettingsEntry> {
    @Override
    public SettingsEntry create(SettingsEntry entry) {
        Check.notNullArgument(entry, "entry");
        try {
            SettingsEntry settingsEntry = find(entry);
            if (settingsEntry == null) {
                Database.getStatement().execute("INSERT INTO SETTINGSTABLE (PROFILE, WIDTH, HEIGHT) VALUES('" + entry.getProfile() + "', '" + entry.getWindowWidth() + "', '" + entry.getWindowHeight() + "')");
                return find(entry);
            } else {
                System.out.println("Settings profile \"" + entry.getProfile() + "\" already exists");
                return settingsEntry;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public SettingsEntry update(SettingsEntry entry) {
        Check.notNullArgument(entry, "entry");

        try {
            Database.getStatement().execute("UPDATE SETTINGSTABLE SET PROFILE = '" + entry.getProfile() + "', WIDTH = '" +
                    entry.getWindowWidth() + "', HEIGHT = '" + entry.getWindowHeight() + "' WHERE ID = '" + entry.getId() + "'");
        } catch (SQLException e) {
            System.err.println("Could not update settings profile: " + entry.getProfile());
            e.printStackTrace();
        }
        return find(entry);
    }

    @Override
    public void remove(SettingsEntry entry) {
    }

    @Override
    public SettingsEntry find(SettingsEntry entry) {
        Check.notNullArgument(entry, "entry");
        try {
            ResultSet result = Database.getStatement().executeQuery("SELECT * FROM SETTINGSTABLE WHERE PROFILE='" + entry.getProfile() + "'");
            if (result.next()) {
                return new SettingsEntry(result.getInt("ID"), result.getString("PROFILE"), result.getInt("WIDTH"), result.getInt("HEIGHT"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public SettingsEntry find(final String profile) {
        return find(new SettingsEntry(profile, 0, 0));
    }

    @Override
    public List<SettingsEntry> findAll() {
        return null;
    }
}
