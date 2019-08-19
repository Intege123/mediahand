package mediahand.core;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import mediahand.MediaLoader;
import mediahand.controller.MediaHandAppController;
import mediahand.domain.DirectoryEntry;
import mediahand.domain.SettingsEntry;
import mediahand.repository.base.Database;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

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
    }

    @Override
    public void stop() {
        int width = (int) MediaHandApp.stage.getWidth();
        int height = (int) MediaHandApp.stage.getHeight();
        if (this.settingsEntry == null) {
            Database.getSettingsRepository().create(new SettingsEntry("default", width, height));
        } else {
            this.settingsEntry.setWindowWidth(width);
            this.settingsEntry.setWindowHeight(height);
            Database.getSettingsRepository().update(this.settingsEntry);
        }
    }

    private void initRootLayout() throws IOException {
        this.rootLayout = FXMLLoader.load(getClass().getResource("/fxml/RootLayout.fxml"));
        MediaHandApp.scene = new Scene(this.rootLayout);
        setDefaultScene();

        this.settingsEntry = Database.getSettingsRepository().find("default");
        if (this.settingsEntry != null) {
            MediaHandApp.stage.setWidth(this.settingsEntry.getWindowWidth());
            MediaHandApp.stage.setHeight(this.settingsEntry.getWindowHeight());
        }

        MediaHandApp.stage.show();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Database.closeConnections();
            System.out.println("Program closed");
        }));
    }

    private void initDatabase() {
        Database.init("AnimeDatabase", "lueko", "1234", false);

        if (Database.getBasePathRepository().findAll().size() == 0) {
            chooseBasePath();
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
            e.printStackTrace();
        }
    }

    public static void setDefaultScene() {
        MediaHandApp.stage.setScene(MediaHandApp.scene);
        MediaHandApp.stage.setTitle(MediaHandApp.MEDIA_HAND_TITLE);
    }

    public static boolean chooseBasePath() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File dialog = directoryChooser.showDialog(MediaHandApp.stage);

        if (dialog != null) {
            MediaHandApp.mediaLoader.addMedia(Database.getBasePathRepository().create(new DirectoryEntry(dialog.getAbsolutePath())));
            return true;
        }
        return false;
    }

    public static Optional<File> chooseMediaDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File dialog = directoryChooser.showDialog(MediaHandApp.getStage());

        return Optional.ofNullable(dialog);
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
