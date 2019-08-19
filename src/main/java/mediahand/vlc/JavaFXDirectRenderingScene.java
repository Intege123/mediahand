package mediahand.vlc;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import javafx.stage.Stage;
import mediahand.core.MediaHandApp;
import mediahand.vlc.event.StopRenderingSceneHandler;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.TrackDescription;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallbackAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

public class JavaFXDirectRenderingScene {

    private static final double FPS = 60.0;

    private final NanoTimer nanoTimer = new NanoTimer(1000.0 / FPS) {
        @Override
        protected void onSucceeded() {
            renderFrame();
        }
    };

    /**
     * Filename of the video to play.
     */
    private final String videoFile;

    /**
     * Lightweight JavaFX canvas, the video is rendered here.
     */
    private final Canvas canvas;

    private Timer timer = new Timer();

    private ContextMenu contextMenu;

    /**
     * Pixel writer to update the canvas.
     */
    private PixelWriter pixelWriter;

    /**
     * Pixel format.
     */
    private final WritablePixelFormat<ByteBuffer> pixelFormat;

    private StackPane stackPane;

    private final MediaPlayerFactory mediaPlayerFactory;

    /**
     * The vlcj direct rendering media player component.
     */
    private EmbeddedMediaPlayer mediaPlayer;

    private Stage stage;

    private WritableImage img;

    private Slider mediaTimeSlider;

    private BorderPane controlPane;

    private final long delay = 1000;

    public JavaFXDirectRenderingScene(final File videoFile) {
        this.videoFile = videoFile.getAbsolutePath();

        this.canvas = new Canvas();

        this.pixelWriter = this.canvas.getGraphicsContext2D().getPixelWriter();
        this.pixelFormat = PixelFormat.getByteBgraInstance();

        this.mediaPlayerFactory = new MediaPlayerFactory();
        this.mediaPlayer = this.mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();

        this.mediaPlayer.videoSurface().set(new JavaFXDirectRenderingScene.JavaFxVideoSurface());

        this.stackPane = new StackPane();
        this.controlPane = new BorderPane();

        this.stackPane.getChildren().add(0, this.canvas);
        this.stackPane.getChildren().add(1, this.controlPane);
        this.stackPane.setStyle("-fx-background-color: rgb(0, 0, 0);");

        this.canvas.widthProperty().bind(this.stackPane.widthProperty());
        this.canvas.heightProperty().bind(this.stackPane.heightProperty());

        addContextMenuListeners();
    }

    public void start(final Stage primaryStage, final String title) {
        initStage(primaryStage, title);

        this.mediaPlayer.controls().setRepeat(false);

        this.mediaPlayer.media().play(this.videoFile);

        if (startTimer()) {
            onMediaLoaded();
        }
    }

    private void initStage(Stage primaryStage, String title) {
        this.stage = primaryStage;

        this.stage.setOnCloseRequest(new StopRenderingSceneHandler(this));

        this.stage.setTitle(title);

        this.stage.setFullScreenExitKeyCombination(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));

        Scene scene = initScene();
        this.stage.setScene(scene);
        this.stage.show();
    }

    private Scene initScene() {
        Scene scene = new Scene(this.stackPane, Color.BLACK);
        addSceneKeyListeners(scene);
        return scene;
    }

    public void stop() {
        this.stage.setOnCloseRequest(null);
        stopTimer();
        this.timer.cancel();

        this.mediaPlayer.controls().stop();
        this.mediaPlayer.release();
        this.mediaPlayerFactory.release();
    }

    private void onMediaFinished() {
        boolean fullScreen = JavaFXDirectRenderingScene.this.stage.isFullScreen();
        stop();
        MediaHandApp.getMediaHandAppController().increaseCurrentEpisode();
        if (MediaHandApp.getMediaHandAppController().autoContinueCheckbox.isSelected()) {
            playSelectedMedia(fullScreen);
        } else {
            MediaHandApp.setDefaultScene();
        }
    }

    private void playSelectedMedia(boolean fullScreen) {
        MediaHandApp.getMediaHandAppController().playEmbeddedMedia();
        JavaFXDirectRenderingScene.this.stage.setFullScreen(fullScreen);
    }

    private void onMediaLoaded() {
        this.contextMenu = buildContextMenu(this.mediaPlayer);

        setMediaPlayerEventListener();

        double mediaDuration = this.mediaPlayer.media().info().duration() / 60000.0;
        BorderPane sliderPane = initSliderPane(mediaDuration);

        this.controlPane.setBottom(sliderPane);
    }

    private void setMediaPlayerEventListener() {
        this.mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
                updateMediaTimeSlider(newTime);
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                Platform.runLater(() -> {
                    onMediaFinished();
                });
            }
        });
    }

    private void updateMediaTimeSlider(long newTime) {
        if (!JavaFXDirectRenderingScene.this.mediaTimeSlider.isPressed()) {
            JavaFXDirectRenderingScene.this.mediaTimeSlider.setValue(newTime / 60000.0);
        }
    }

    private BorderPane initSliderPane(double mediaDuration) {
        this.mediaTimeSlider = initMediaTimeSlider(mediaDuration, this.mediaPlayer);
        Slider volumeSlider = initVolumeSlider(this.mediaPlayer);

        BorderPane sliderPane = new BorderPane(this.mediaTimeSlider);
        sliderPane.setRight(volumeSlider);

        addControlPaneMouseListener(sliderPane);
        return sliderPane;
    }

    private void addControlPaneMouseListener(BorderPane sliderPane) {
        this.stackPane.setOnMouseMoved(event -> {
            this.controlPane.setVisible(true);
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    JavaFXDirectRenderingScene.this.controlPane.setVisible(false);
                }
            };
            this.timer.cancel();
            if (sliderPane.sceneToLocal(event.getSceneX(), event.getSceneY()).getY() < -10) {
                this.timer = new Timer();
                this.timer.schedule(task, this.delay);
            }
        });
    }

    private Slider initMediaTimeSlider(final double mediaDuration, final EmbeddedMediaPlayer mediaPlayer) {
        Slider slider = new Slider(0, mediaDuration, 0);
        slider.setMajorTickUnit(mediaDuration);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setOnMouseClicked(event -> mediaPlayer.controls().setTime((long) (slider.getValue() * 60000)));
        return slider;
    }

    private Slider initVolumeSlider(final EmbeddedMediaPlayer mediaPlayer) {
        Slider volumeSlider = new Slider(0, 100, 50);
        mediaPlayer.audio().setVolume((int) volumeSlider.getValue());
        volumeSlider.setOnMouseClicked(event -> mediaPlayer.audio().setVolume((int) volumeSlider.getValue()));
        volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> mediaPlayer.audio().setVolume(newValue.intValue()));
        return volumeSlider;
    }

    private void addSceneKeyListeners(final Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stop();
                MediaHandApp.setDefaultScene();
            } else if (event.getCode() == KeyCode.SPACE) {
                this.mediaPlayer.controls().pause();
            } else if (event.getCode() == KeyCode.ENTER) {
                this.mediaPlayer.controls().skipTime(80000);
            } else if (!event.isControlDown() && event.getCode() == KeyCode.F) {
                this.stage.setFullScreen(true);
            } else if (event.getCode() == KeyCode.UP) {
                playNextEpisode();
            } else if (event.getCode() == KeyCode.DOWN) {
                playPreviousEpisode();
            }
        });
    }

    private void playNextEpisode() {
        boolean fullScreen = this.stage.isFullScreen();
        stop();
        MediaHandApp.getMediaHandAppController().increaseCurrentEpisode();
        playSelectedMedia(fullScreen);
    }

    private void playPreviousEpisode() {
        boolean fullScreen = this.stage.isFullScreen();
        stop();
        MediaHandApp.getMediaHandAppController().decreaseCurrentEpisode();
        playSelectedMedia(fullScreen);
    }

    private void addContextMenuListeners() {
        addShowContextMenuListener();
        addHideContextMenuListener();
    }

    private void addShowContextMenuListener() {
        this.stackPane.setOnContextMenuRequested(event -> this.contextMenu.show(this.stackPane, event.getScreenX(), event.getScreenY()));
    }

    private void addHideContextMenuListener() {
        this.stackPane.setOnMouseClicked(event -> {
            if (this.contextMenu != null) {
                this.contextMenu.hide();
            }
        });
    }

    private ContextMenu buildContextMenu(final EmbeddedMediaPlayer mediaPlayer) {
        Menu audioMenu = buildAudioContextMenu(mediaPlayer);

        Menu subtitleMenu = buildSubtitleContextMenu(mediaPlayer);

        return new ContextMenu(audioMenu, subtitleMenu);
    }

    private Menu buildSubtitleContextMenu(EmbeddedMediaPlayer mediaPlayer) {
        Menu subtitleMenu = new Menu("Subtitle");
        for (TrackDescription trackDescription : this.mediaPlayer.subpictures().trackDescriptions()) {
            MenuItem item = initSubtitleMenuItem(mediaPlayer, trackDescription);
            subtitleMenu.getItems().add(item);
        }
        return subtitleMenu;
    }

    private MenuItem initSubtitleMenuItem(EmbeddedMediaPlayer mediaPlayer, TrackDescription trackDescription) {
        MenuItem item = new MenuItem(trackDescription.description());
        item.setId(trackDescription.id() + "");
        if (trackDescription.id() == mediaPlayer.subpictures().track()) {
            highlightMenuItem(item);
        }
        item.setOnAction(event -> {
            setSubtitleTrack(mediaPlayer, trackDescription, item);
        });
        return item;
    }

    private void setSubtitleTrack(EmbeddedMediaPlayer mediaPlayer, TrackDescription trackDescription, MenuItem item) {
        resetStyleOfCurrentSubtitleTrack(mediaPlayer);
        mediaPlayer.subpictures().setTrack(trackDescription.id());
        highlightMenuItem(item);
    }

    private Menu buildAudioContextMenu(EmbeddedMediaPlayer mediaPlayer) {
        Menu audioMenu = new Menu("Audio");
        for (TrackDescription trackDescription : this.mediaPlayer.audio().trackDescriptions()) {
            MenuItem item = initAudioMenuItem(mediaPlayer, trackDescription);
            audioMenu.getItems().add(item);
        }
        return audioMenu;
    }

    private MenuItem initAudioMenuItem(EmbeddedMediaPlayer mediaPlayer, TrackDescription trackDescription) {
        MenuItem item = new MenuItem(trackDescription.description());
        item.setId(trackDescription.id() + "");
        if (trackDescription.id() == mediaPlayer.audio().track()) {
            highlightMenuItem(item);
        }
        item.setOnAction(event -> {
            setAudioTrack(mediaPlayer, trackDescription, item);
        });
        return item;
    }

    private void setAudioTrack(EmbeddedMediaPlayer mediaPlayer, TrackDescription trackDescription, MenuItem item) {
        resetStyleOfCurrentAudioTrack(mediaPlayer);
        mediaPlayer.audio().setTrack(trackDescription.id());
        highlightMenuItem(item);
    }

    private void highlightMenuItem(MenuItem item) {
        item.setStyle("-fx-text-fill: green;");
    }

    private void resetStyleOfCurrentAudioTrack(final EmbeddedMediaPlayer mediaPlayer) {
        Menu audioMenu = (Menu) this.contextMenu.getItems().get(0);
        audioMenu.getItems().filtered(item1 -> item1.getId().equals(mediaPlayer.audio().track() + "")).get(0).setStyle("-fx-text-fill: black;");
    }

    private void resetStyleOfCurrentSubtitleTrack(final EmbeddedMediaPlayer mediaPlayer) {
        Menu subtitleMenu = (Menu) this.contextMenu.getItems().get(1);
        subtitleMenu.getItems().filtered(item1 -> item1.getId().equals(mediaPlayer.subpictures().track() + "")).get(0).setStyle("-fx-text-fill: black;");
    }

    private class JavaFxVideoSurface extends CallbackVideoSurface {

        JavaFxVideoSurface() {
            super(new JavaFXDirectRenderingScene.JavaFxBufferFormatCallback(), new JavaFXDirectRenderingScene.JavaFxRenderCallback(), true, VideoSurfaceAdapters.getVideoSurfaceAdapter());
        }

    }

    private class JavaFxBufferFormatCallback extends BufferFormatCallbackAdapter {
        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            JavaFXDirectRenderingScene.this.img = new WritableImage(sourceWidth, sourceHeight);
            JavaFXDirectRenderingScene.this.pixelWriter = JavaFXDirectRenderingScene.this.img.getPixelWriter();

            return new RV32BufferFormat(sourceWidth, sourceHeight);
        }
    }

    // Semaphore used to prevent the pixel writer from being updated in one thread while it is being rendered by a
    // different thread
    private final Semaphore semaphore = new Semaphore(1);

    // This is correct as far as it goes, but we need to use one of the timers to get smooth rendering (the timer is
    // handled by the demo sub-classes)
    private class JavaFxRenderCallback implements RenderCallback {
        @Override
        public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
            try {
                JavaFXDirectRenderingScene.this.semaphore.acquire();
                JavaFXDirectRenderingScene.this.pixelWriter.setPixels(0, 0, bufferFormat.getWidth(), bufferFormat.getHeight(), JavaFXDirectRenderingScene.this.pixelFormat, nativeBuffers[0], bufferFormat.getPitches()[0]);
                JavaFXDirectRenderingScene.this.semaphore.release();
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private void renderFrame() {
        GraphicsContext g = this.canvas.getGraphicsContext2D();

        double width = this.canvas.getWidth();
        double height = this.canvas.getHeight();

        g.setFill(new Color(0, 0, 0, 1));
        g.fillRect(0, 0, width, height);

        if (this.img != null) {
            double imageWidth = this.img.getWidth();
            double imageHeight = this.img.getHeight();

            double sx = width / imageWidth;
            double sy = height / imageHeight;

            double sf = Math.min(sx, sy);

            double scaledW = imageWidth * sf;
            double scaledH = imageHeight * sf;

            Affine ax = g.getTransform();

            g.translate(
                    (width - scaledW) / 2,
                    (height - scaledH) / 2
            );

            if (sf != 1.0) {
                g.scale(sf, sf);
            }

            try {
                this.semaphore.acquire();
                g.drawImage(this.img, 0, 0);
                this.semaphore.release();
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
            }

            g.setTransform(ax);
        }
    }

    private boolean startTimer() {
        this.nanoTimer.start();
        // wait for the media to load, because the api call to vlc is async
        while (this.mediaPlayer.media().info().duration() == -1 || this.mediaPlayer.audio().volume() == -1) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void stopTimer() {
        this.nanoTimer.cancel();
    }
}
