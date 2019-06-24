package mediahand.repository.base;

import mediahand.repository.BasePathRepository;
import mediahand.repository.MediaRepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/*Tables:
 *
 * "mediaTable"
 * "dirTable"
 * "mediaState"
 *
 */

public abstract class Database {

    private static Connection connection;
    private static Statement statement;

    private static BasePathRepository basePathRepository;
    private static MediaRepository mediaRepository;

    /**
     * Connects to the specified database and creates or opens the mediaTable.
     *
     * @param databaseName   the name of the database
     * @param username       the username to connect to the database
     * @param password       the password to connect to the database
     * @param removeOldTable Determines whether the old table should be removed to create a new table or not.
     */
    public static void init(String databaseName, String username, String password, boolean removeOldTable) {

        openConnection(databaseName, username, password);

        /*
         * Removing old table to create a new table.
         */
        if (removeOldTable) {
            dropTables();
        }

        openDirTable();
        openMediaTable();

        basePathRepository = new BasePathRepository();
        mediaRepository = new MediaRepository();
    }

    /**
     * Connects to the specified database.
     *
     * @param databaseName the name of the database to connect to
     * @param username     the username to connect to the database
     * @param password     the password to connect to the database
     */
    public static void openConnection(final String databaseName, final String username, final String password) {
        /*
         * Checking for JDBC driver.
         */
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver not found!");
            System.exit(-1);
        }

        /*
         * Connecting to local database.
         */
        try {
            connection = DriverManager.getConnection("jdbc:hsqldb:file:" + databaseName + "; shutdown = true", username, password);
            statement = connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Closes all connections of the connected database.
     */
    public static void closeConnections() {
        try {
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates or opens the media table to allow writing and reading.
     */
    public static void openMediaTable() {
        if (statement != null) {
            try {
                statement.execute("CREATE TABLE mediaTable(id INT IDENTITY PRIMARY KEY, Title VARCHAR(255) UNIQUE, Episodes INT NOT NULL, " +
                        "MediaType VARCHAR(255) NOT NULL, WatchState VARCHAR(255) NOT NULL, Rating INT, " +
                        "Path VARCHAR(255) NOT NULL, CurrentEpisode INT DEFAULT 1 NOT NULL, " +
                        "Added DATE DEFAULT SYSDATE NOT NULL, EpisodeLength INT NOT NULL, " +
                        "WatchedDate DATE, WatchNumber INT, dirtable_fk INT NOT NULL, FOREIGN KEY (dirtable_fk) REFERENCES DIRTABLE(id))");
                System.out.println("Opened new media table!");
            } catch (SQLException e) {
                System.err.println(e);
                try {
                    statement.execute("TABLE mediaTable");
                } catch (SQLException e2) {
                    System.err.println("Could not open mediaTable!");
                    e2.printStackTrace();
                }
            }
        } else {
            System.err.println("Could not open mediaTable. Statement is null!");
        }
    }

    /**
     * Creates or opens the directory table to allow writing and reading.
     */
    public static void openDirTable() {
        if (statement != null) {
            try {
                statement.execute("CREATE TABLE dirTable(ID INT IDENTITY PRIMARY KEY, " +
                        "PATH VARCHAR(255) NOT NULL)");
                System.out.println("Opened new directory table!");
            } catch (SQLException e) {
                System.err.println(e);
                try {
                    statement.execute("TABLE dirTable");
                } catch (SQLException e2) {
                    System.err.println("Could not open dirTable!");
                    e2.printStackTrace();
                }
            }
        }
    }

    public static void dropTables() {
        try {
            statement.execute("DROP TABLE mediaTable");
            statement.execute("DROP TABLE dirTable");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void printTables() {
        DBTablePrinter.printTable(connection, "mediaTable");
        DBTablePrinter.printTable(connection, "dirTable");
    }

    public static Statement getStatement() {
        return statement;
    }

    public static BasePathRepository getBasePathRepository() {
        return basePathRepository;
    }

    public static MediaRepository getMediaRepository() {
        return mediaRepository;
    }
}
