package mediahand.repository.base;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import mediahand.repository.BasePathRepository;
import mediahand.repository.MediaRepository;
import mediahand.repository.SettingsRepository;
import mediahand.utils.MessageUtil;

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
    private static SettingsRepository settingsRepository;

    /**
     * Connects to the specified database and creates or opens the mediaTable.
     *
     * @param databaseName the name of the database
     * @param username the username to connect to the database
     * @param password the password to connect to the database
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
        openSettingsTable();

        basePathRepository = new BasePathRepository();
        mediaRepository = new MediaRepository();
        settingsRepository = new SettingsRepository();
    }

    /**
     * Connects to the specified database.
     *
     * @param databaseName the name of the database to connect to
     * @param username the username to connect to the database
     * @param password the password to connect to the database
     */
    public static void openConnection(final String databaseName, final String username, final String password) {
        /*
         * Checking for JDBC driver.
         */
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (ClassNotFoundException e) {
            MessageUtil.warningAlert(e, "Driver not found!");
            System.exit(-1);
        }

        /*
         * Connecting to local database.
         */
        try {
            connection = DriverManager.getConnection(
                    "jdbc:hsqldb:file:" + databaseName + "; shutdown = true", username, password);
            statement = connection.createStatement();
        } catch (SQLException e) {
            MessageUtil.warningAlert(e);
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
            MessageUtil.warningAlert(e);
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                MessageUtil.warningAlert(e);
            }
        }
    }

    /**
     * Creates or opens the media table to allow writing and reading.
     */
    public static void openMediaTable() {
        if (statement != null) {
            try {
                statement.execute(
                        "CREATE TABLE mediaTable(id INT IDENTITY PRIMARY KEY, Title VARCHAR(255) UNIQUE, Episodes INT NOT NULL, "
                                +
                                "MediaType VARCHAR(255) NOT NULL, WatchState VARCHAR(255) NOT NULL, Rating INT, " +
                                "Path VARCHAR(255) NOT NULL, CurrentEpisode INT DEFAULT 1 NOT NULL, " +
                                "Added DATE DEFAULT SYSDATE NOT NULL, EpisodeLength INT NOT NULL, " +
                                "WatchedDate DATE, WatchNumber INT, dirtable_fk INT, Volume INT, Audiotrack VARCHAR(255), Subtitletrack VARCHAR(255), FOREIGN KEY (dirtable_fk) REFERENCES DIRTABLE(id))");
                MessageUtil.infoAlert("openMediaTable", "Opened new media table!");
            } catch (SQLException e) {
                try {
                    statement.execute("TABLE mediaTable");
                } catch (SQLException e2) {
                    MessageUtil.warningAlert(e2, "Could not open mediaTable!");
                }
            }
        } else {
            MessageUtil.warningAlert("openMediaTable", "Could not open mediaTable. Statement is null!");
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
                MessageUtil.infoAlert("openDirTable", "Opened new directory table!");
            } catch (SQLException e) {
                try {
                    statement.execute("TABLE dirTable");
                } catch (SQLException e2) {
                    MessageUtil.warningAlert(e2, "Could not open dirTable!");
                }
            }
        }
    }

    public static void openSettingsTable() {
        if (statement != null) {
            try {
                statement.execute(
                        "CREATE TABLE settingsTable(ID INT IDENTITY PRIMARY KEY, PROFILE VARCHAR(255) NOT NULL UNIQUE, "
                                + "WIDTH INT NOT NULL, HEIGHT INT NOT NULL, AUTOCONTINUE BOOLEAN, SHOWALL BOOLEAN, WATCHSTATE VARCHAR(255))");
                MessageUtil.infoAlert("openSettingsTable", "Opened new settings table!");
            } catch (SQLException e) {
                try {
                    statement.execute("TABLE settingsTable");
                } catch (SQLException e2) {
                    MessageUtil.warningAlert(e2, "Could not open dirTable!");
                }
            }
        }
    }

    public static void dropTables() {
        try {
            statement.execute("DROP TABLE mediaTable");
            statement.execute("DROP TABLE dirTable");
            statement.execute("DROP TABLE settingsTable");
        } catch (SQLException e) {
            MessageUtil.warningAlert(e, "Could not drop tables!");
        }
    }

    public static void printTables() {
        DBTablePrinter.printTable(connection, "mediaTable");
        DBTablePrinter.printTable(connection, "dirTable");
        DBTablePrinter.printTable(connection, "settingsTable");
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

    public static SettingsRepository getSettingsRepository() {
        return settingsRepository;
    }
}
