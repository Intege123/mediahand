package mediahand.vlc.event;

import java.util.List;

import javafx.event.EventHandler;
import javafx.stage.WindowEvent;
import mediahand.vlc.MediaPlayerComponent;

public class StopRenderingSceneHandler implements EventHandler<WindowEvent> {

    private final List<MediaPlayerComponent> mediaPlayerComponents;

    public StopRenderingSceneHandler(final List<MediaPlayerComponent> mediaPlayerComponents) {
        this.mediaPlayerComponents = mediaPlayerComponents;
    }

    @Override
    public void handle(WindowEvent event) {
        for (MediaPlayerComponent mediaPlayerComponent : this.mediaPlayerComponents) {
            mediaPlayerComponent.stop();
        }
    }

}
