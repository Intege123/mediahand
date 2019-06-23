package mediahand.controller;

import javafx.event.ActionEvent;
import mediahand.core.MediaHandApp;

public class RootLayoutController {

    public void addDirectory(ActionEvent actionEvent) {
        MediaHandApp.chooseBasePath();
        MediaHandApp.getMediaHandAppController().fillTableView();
    }
}
