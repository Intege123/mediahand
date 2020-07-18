package mediahand.vlc;

import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import mediahand.core.MediaHandApp;
import mediahand.domain.MediaEntry;
import mediahand.repository.RepositoryFactory;
import mediahand.utils.MessageUtil;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class ControlPane implements MediaPlayerComponent {

    private static final long DELAY = 1000;

    private final BorderPane borderPane;

    private final EmbeddedMediaPlayer embeddedMediaPlayer;

    private Slider mediaTimeSlider;

    private MediaEntry mediaEntry;

    private Timer timer = new Timer();

    public ControlPane(final EmbeddedMediaPlayer embeddedMediaPlayer, final Scene scene) {
        this.borderPane = new BorderPane();
        this.embeddedMediaPlayer = embeddedMediaPlayer;

        registerTimeListener();
        registerKeyControlListeners(scene);
    }

    public BorderPane getBorderPane() {
        return this.borderPane;
    }

    public void update(final MediaEntry mediaEntry) {
        this.mediaEntry = mediaEntry;
        double mediaDuration = this.embeddedMediaPlayer.media().info().duration() / 60000.0;
        BorderPane sliderPane = initSliderPane(mediaDuration);
        showTimedTimeSlider(ControlPane.DELAY * 3);

        this.borderPane.setBottom(sliderPane);
    }

    private void registerKeyControlListeners(final Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stop();
                MediaHandApp.setDefaultScene();
            } else if (event.getCode() == KeyCode.SPACE) {
                this.embeddedMediaPlayer.controls().pause();
                showTimedTimeSlider(ControlPane.DELAY * 3);
            } else if (event.getCode() == KeyCode.ENTER) {
                this.embeddedMediaPlayer.controls().skipTime(80000);
            } else if (!event.isControlDown() && event.getCode() == KeyCode.F) {
                MediaHandApp.getStage().setFullScreen(true);
            } else if (event.getCode() == KeyCode.UP) {
                playNextEpisode();
            } else if (event.getCode() == KeyCode.DOWN) {
                playPreviousEpisode();
            }
        });
    }

    public void stop() {
        if (this.embeddedMediaPlayer.status().isPlaying()) {
            this.embeddedMediaPlayer.controls().stop();
        }
        this.timer.cancel();
        RepositoryFactory.getMediaRepository().update(this.mediaEntry);
    }

    private void playSelectedMedia(boolean fullScreen) {
        MediaHandApp.getMediaHandAppController().playEmbeddedMedia();
        MediaHandApp.getStage().setFullScreen(fullScreen);
    }

    private void playNextEpisode() {
        stop();
        boolean fullScreen = MediaHandApp.getStage().isFullScreen();
        MediaHandApp.getMediaHandAppController().increaseCurrentEpisode();
        playSelectedMedia(fullScreen);
    }

    private void playPreviousEpisode() {
        stop();
        boolean fullScreen = MediaHandApp.getStage().isFullScreen();
        MediaHandApp.getMediaHandAppController().decreaseCurrentEpisode();
        playSelectedMedia(fullScreen);
    }

    private void registerTimeListener() {
        this.embeddedMediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void timeChanged(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer, long newTime) {
                Platform.runLater(() -> updateMediaTimeSlider(newTime));
            }
        });
    }

    private void updateMediaTimeSlider(long newTime) {
        if (this.mediaTimeSlider != null && !this.mediaTimeSlider.isPressed()) {
            this.mediaTimeSlider.setValue(newTime / 60000.0);
        }
    }

    private BorderPane initSliderPane(final double mediaDuration) {
        this.mediaTimeSlider = initMediaTimeSlider(mediaDuration);
        Slider volumeSlider = initVolumeSlider(this.mediaEntry.getVolume());

        BorderPane sliderPane = new BorderPane(this.mediaTimeSlider);
        sliderPane.setRight(volumeSlider);

        registerControlPaneMouseListener(sliderPane);
        return sliderPane;
    }

    private void registerControlPaneMouseListener(final BorderPane sliderPane) {
        Parent parent = this.borderPane.getParent();
        if (parent != null) {
            parent.setOnMouseMoved(event -> {
                showTimedTimeSlider(ControlPane.DELAY);
                TimerTask timerTask = showTimeSlider();
                if (sliderPane.sceneToLocal(event.getSceneX(), event.getSceneY()).getY() < -10) {
                    this.timer = new Timer();
                    this.timer.schedule(timerTask, ControlPane.DELAY);
                }
            });
        } else {
            MessageUtil.warningAlert("Control pane", "Could not add control pane, because it has not been added to a scene graph.");
        }
    }

    private Slider initMediaTimeSlider(final double mediaDuration) {
        Slider slider = new Slider(0, mediaDuration, 0);
        slider.setMajorTickUnit(mediaDuration);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setOnMouseClicked(event -> this.embeddedMediaPlayer.controls().setTime((long) (slider.getValue()
                * 60000)));
        return slider;
    }

    private Slider initVolumeSlider(final int volume) {
        Slider volumeSlider = new Slider(0, 100, volume);
        this.embeddedMediaPlayer.audio().setVolume((int) volumeSlider.getValue());
        volumeSlider.setOnMouseClicked(event -> {
            int newVolume = (int) volumeSlider.getValue();
            this.embeddedMediaPlayer.audio().setVolume(newVolume);
            this.mediaEntry.setVolume(newVolume);
        });
        volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> this.embeddedMediaPlayer.audio().setVolume(newValue.intValue()));
        return volumeSlider;
    }

    private void showTimedTimeSlider(final long delay) {
        TimerTask timerTask = showTimeSlider();
        this.timer = new Timer();
        this.timer.schedule(timerTask, delay);
    }

    private TimerTask showTimeSlider() {
        this.borderPane.setVisible(true);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                ControlPane.this.borderPane.setVisible(false);
            }
        };
        this.timer.cancel();
        return task;
    }

}
