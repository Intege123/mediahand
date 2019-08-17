package mediahand.repository;

import mediahand.domain.DirectoryEntry;
import mediahand.repository.base.BaseRepository;
import mediahand.repository.base.Database;
import utils.Check;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BasePathRepository implements BaseRepository<DirectoryEntry> {

    @Override
    public DirectoryEntry create(DirectoryEntry entry) {
        Check.notNullArgument(entry, "entry");
        try {
            DirectoryEntry directoryEntry = find(entry);
            if (directoryEntry == null) {
                Database.getStatement().execute("INSERT INTO dirTable (Path) VALUES('" + entry.getPath() + "')");
                return find(entry);
            } else {
                System.out.println("Directory \"" + entry.getPath() + "\" already exists");
                return directoryEntry;
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
        try {
            ResultSet result = Database.getStatement().executeQuery("SELECT * FROM DIRTABLE WHERE PATH='" + entry.getPath() + "'");
            if (result.next()) {
                return new DirectoryEntry(result.getInt("ID"), result.getString("PATH"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<DirectoryEntry> findAll() {
        List<DirectoryEntry> directoryEntries = new ArrayList<>();

        try {
            ResultSet result = Database.getStatement().executeQuery("SELECT * FROM DIRTABLE");
            while (result.next()) {
                directoryEntries.add(new DirectoryEntry(result.getInt("ID"), result.getString("PATH")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return directoryEntries;
    }

}
