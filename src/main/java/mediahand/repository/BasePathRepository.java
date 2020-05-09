package mediahand.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mediahand.domain.DirectoryEntry;
import mediahand.repository.base.BaseRepository;
import mediahand.repository.base.Database;
import mediahand.utils.MessageUtil;
import utils.Check;

public class BasePathRepository implements BaseRepository<DirectoryEntry> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePathRepository.class);

    @Override
    public DirectoryEntry create(DirectoryEntry entry) {
        Check.notNullArgument(entry, "entry");
        try {
            DirectoryEntry directoryEntry = find(entry);
            if (directoryEntry == null) {
                Database.getStatement().execute("INSERT INTO dirTable (Path) VALUES('" + entry.getPath() + "')");
                return find(entry);
            } else {
                MessageUtil.infoAlert("create directory", "Directory \"" + entry.getPath() + "\" already exists");
                return directoryEntry;
            }
        } catch (SQLException e) {
            LOGGER.error("create", e);
        }
        return null;
    }

    @Override
    public DirectoryEntry update(DirectoryEntry entry) {
        return null;
    }

    @Override
    public void remove(DirectoryEntry entry) {
    }

    @Override
    public DirectoryEntry find(DirectoryEntry entry) {
        Check.notNullArgument(entry, "entry");
        try (ResultSet result = Database.getStatement().executeQuery(
                "SELECT * FROM DIRTABLE WHERE PATH='" + entry.getPath() + "'")) {
            if (result.next()) {
                return new DirectoryEntry(result.getInt("ID"), result.getString("PATH"));
            }
        } catch (SQLException e) {
            LOGGER.error("find", e);
        }
        return null;
    }

    @Override
    public List<DirectoryEntry> findAll() {
        List<DirectoryEntry> directoryEntries = new ArrayList<>();

        try (ResultSet result = Database.getStatement().executeQuery("SELECT * FROM DIRTABLE")) {
            while (result.next()) {
                directoryEntries.add(new DirectoryEntry(result.getInt("ID"), result.getString("PATH")));
            }
        } catch (SQLException e) {
            LOGGER.error("findAll", e);
        }
        return directoryEntries;
    }

}
