package mediahand.vlc.event;

import javafx.event.EventHandler;
import javafx.stage.WindowEvent;
import mediahand.vlc.JavaFXDirectRenderingScene;

public class StopRenderingSceneHandler implements EventHandler<WindowEvent> {

    private JavaFXDirectRenderingScene javaFXDirectRenderingScene;

    public StopRenderingSceneHandler(final JavaFXDirectRenderingScene javaFXDirectRenderingScene) {
        this.javaFXDirectRenderingScene = javaFXDirectRenderingScene;
    }

    @Override
    public void handle(WindowEvent event) {
        this.javaFXDirectRenderingScene.stop();
    }

}
