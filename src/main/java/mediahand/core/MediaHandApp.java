package mediahand.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import mediahand.MediaLoader;
import mediahand.WatchState;
import mediahand.controller.MediaHandAppController;
import mediahand.domain.DirectoryEntry;
import mediahand.domain.SettingsEntry;
import mediahand.repository.base.Database;
import mediahand.utils.MessageUtil;

public class MediaHandApp extends Application {

    public static final String MEDIA_HAND_TITLE = "Media Hand";

    private static Stage stage;
    private static MediaLoader mediaLoader;
    private static MediaHandAppController mediaHandAppController;
    private static Scene scene;

    private BorderPane rootLayout;
    private SettingsEntry settingsEntry;

    @Override
    public void start(Stage stage) throws IOException {
        MediaHandApp.stage = stage;
        MediaHandApp.mediaLoader = new MediaLoader();

        initDatabase();

        initRootLayout();

        showMediaHand();

        applyFilterSettings();
    }

    @Override
    public void stop() {
        int width = (int) MediaHandApp.stage.getWidth();
        int height = (int) MediaHandApp.stage.getHeight();
        this.settingsEntry.setWindowWidth(width);
        this.settingsEntry.setWindowHeight(height);
        this.settingsEntry.setAutoContinue(mediaHandAppController.autoContinueCheckbox.isSelected());
        this.settingsEntry.setShowAll(mediaHandAppController.showAllCheckbox.isSelected());
        this.settingsEntry.setWatchState(WatchState.lookupByName(mediaHandAppController.watchStateFilter.getSelectionModel().getSelectedItem()));
        Database.getSettingsRepository().update(this.settingsEntry);
    }

    private void initRootLayout() throws IOException {
        this.rootLayout = FXMLLoader.load(getClass().getResource("/fxml/RootLayout.fxml"));
        MediaHandApp.scene = new Scene(this.rootLayout);
        setDefaultScene();

        this.settingsEntry = Database.getSettingsRepository().create(new SettingsEntry("default", (int) MediaHandApp.stage.getWidth(), (int) MediaHandApp.stage.getHeight(), false, false, null));
        MediaHandApp.stage.setWidth(this.settingsEntry.getWindowWidth());
        MediaHandApp.stage.setHeight(this.settingsEntry.getWindowHeight());

        MediaHandApp.stage.show();

        Runtime.getRuntime().addShutdownHook(new Thread(Database::closeConnections));
    }

    private void initDatabase() {
        Database.init("AnimeDatabase", "lueko", "1234", false);

        if (Database.getBasePathRepository().findAll().size() == 0) {
            addBasePath();
        }
    }

    private void showMediaHand() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MediaHandApp.class.getResource("/fxml/mediaHandApp.fxml"));
            this.rootLayout.setCenter(loader.load());
            MediaHandApp.mediaHandAppController = loader.getController();
            mediaHandAppController.init();
        } catch (IOException e) {
            MessageUtil.warningAlert(e);
        }
    }

    private void applyFilterSettings() {
        mediaHandAppController.autoContinueCheckbox.setSelected(this.settingsEntry.isAutoContinue());
        mediaHandAppController.showAllCheckbox.setSelected(this.settingsEntry.isShowAll());
        mediaHandAppController.watchStateFilter.getSelectionModel().select(this.settingsEntry.getWatchStateValue());
        mediaHandAppController.onFilter(null);
    }

    public static void setDefaultScene() {
        MediaHandApp.stage.setScene(MediaHandApp.scene);
        MediaHandApp.stage.setTitle(MediaHandApp.MEDIA_HAND_TITLE);
    }

    public static boolean addBasePath() {
        Optional<File> baseDir = chooseMediaDirectory();

        if (baseDir.isPresent()) {
            MediaHandApp.mediaLoader.addMedia(Database.getBasePathRepository().create(new DirectoryEntry(baseDir.get().getAbsolutePath())));
            return true;
        }
        return false;
    }

    /**
     * Opens a dialog to choose a directory of the file system.
     *
     * @return the chosen directory
     */
    public static Optional<File> chooseMediaDirectory(final Path initialDirPath) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        if (initialDirPath != null) {
            directoryChooser.setInitialDirectory(initialDirPath.toFile());
        }
        File dialog = directoryChooser.showDialog(MediaHandApp.getStage());

        return Optional.ofNullable(dialog);
    }

    /**
     * Opens a dialog to choose a directory of the file system.
     *
     * @return the chosen directory
     */
    public static Optional<File> chooseMediaDirectory() {
        return chooseMediaDirectory(null);
    }

    public static MediaHandAppController getMediaHandAppController() {
        return MediaHandApp.mediaHandAppController;
    }

    public static MediaLoader getMediaLoader() {
        return MediaHandApp.mediaLoader;
    }

    public static Stage getStage() {
        return stage;
    }

    public static Scene getScene() {
        return scene;
    }
}
