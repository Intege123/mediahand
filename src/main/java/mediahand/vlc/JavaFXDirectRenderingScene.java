package mediahand.vlc;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
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
import java.util.List;
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

    private List<TrackDescription> trackDescriptions;

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

    private Scene scene;

    public JavaFXDirectRenderingScene(final File videoFile) {
        this.videoFile = videoFile.getAbsolutePath();

        this.canvas = new Canvas();

        this.canvas.setFocusTraversable(true);
        this.canvas.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stop();
                MediaHandApp.getStage().setScene(MediaHandApp.getScene());
            } else if (event.getCode() == KeyCode.SPACE) {
                this.mediaPlayer.controls().pause();
            } else if (event.getCode() == KeyCode.ENTER) {
                this.mediaPlayer.controls().skipTime(80000);
            } else if (!event.isControlDown() && event.getCode() == KeyCode.F) {
                this.stage.setFullScreen(true);
            }
        });

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

    public void start(Stage primaryStage) {
        this.stage = primaryStage;

        this.stage.setOnCloseRequest(event -> stop());

        this.stage.setTitle("vlcj JavaFX Direct Rendering");

        this.stage.setFullScreenExitKeyCombination(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));

        this.scene = new Scene(this.stackPane, Color.BLACK);
        primaryStage.setScene(this.scene);
        primaryStage.show();

        this.mediaPlayer.controls().setRepeat(true);

        this.mediaPlayer.media().play(this.videoFile);

        if (startTimer()) {
            onMediaLoaded();
        }
    }

    private void onMediaLoaded() {
        this.trackDescriptions = this.mediaPlayer.audio().trackDescriptions();
        this.contextMenu = buildContextMenu();
        this.mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
                if (!JavaFXDirectRenderingScene.this.mediaTimeSlider.isPressed()) {
                    JavaFXDirectRenderingScene.this.mediaTimeSlider.setValue(newTime / 60000.0);
                }
            }
        });
        double mediaDuration = this.mediaPlayer.media().info().duration() / 60000.0;
        this.mediaTimeSlider = new Slider(0, mediaDuration, 0);
        this.mediaTimeSlider.setMajorTickUnit(mediaDuration);
        this.mediaTimeSlider.setShowTickLabels(true);
        this.mediaTimeSlider.setShowTickMarks(true);
        this.mediaTimeSlider.setOnMouseClicked(event -> this.mediaPlayer.controls().setTime((long) (this.mediaTimeSlider.getValue() * 60000)));

        Slider volumeSlider = new Slider(0, 100, 50);
        this.mediaPlayer.audio().setVolume((int) volumeSlider.getValue());
        volumeSlider.setOnMouseClicked(event -> this.mediaPlayer.audio().setVolume((int) volumeSlider.getValue()));

        BorderPane sliderPane = new BorderPane(this.mediaTimeSlider);
        sliderPane.setRight(volumeSlider);

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

        this.controlPane.setBottom(sliderPane);
    }

    public void stop() {
        stopTimer();
        this.timer.cancel();

        this.mediaPlayer.controls().stop();
        this.mediaPlayer.release();
        this.mediaPlayerFactory.release();
    }

    private void addContextMenuListeners() {
        addShowContextMenuListener();
        addHideContextMenuListener();
    }

    private void addShowContextMenuListener() {
        this.stackPane.setOnContextMenuRequested(event -> {
            this.contextMenu.show(this.stackPane, event.getScreenX(), event.getScreenY());
        });
    }

    private void addHideContextMenuListener() {
        this.stackPane.setOnMouseClicked(event -> {
            if (this.contextMenu != null) {
                this.contextMenu.hide();
            }
        });
    }

    private ContextMenu buildContextMenu() {
        // Create ContextMenu
        ContextMenu contextMenu = new ContextMenu();

        for (TrackDescription trackDescription : this.trackDescriptions) {
            MenuItem item = new MenuItem(trackDescription.description());
            item.setId(trackDescription.id() + "");
            if (trackDescription.id() == this.mediaPlayer.audio().track()) {
                item.setStyle("-fx-text-fill: green;");
            }
            item.setOnAction(event -> {
                resetStyleOfCurrentTrack();
                this.mediaPlayer.audio().setTrack(trackDescription.id());
                item.setStyle("-fx-text-fill: green;");
            });
            contextMenu.getItems().add(item);
        }
        return contextMenu;
    }

    private void resetStyleOfCurrentTrack() {
        this.contextMenu.getItems().filtered(item1 -> item1.getId().equals(this.mediaPlayer.audio().track() + "")).get(0).setStyle("-fx-text-fill: black;");
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

            Platform.runLater(() -> {
                JavaFXDirectRenderingScene.this.stage.setWidth(sourceWidth);
                JavaFXDirectRenderingScene.this.stage.setHeight(sourceHeight);
            });
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
