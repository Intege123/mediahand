package mediahand.controller;

import javafx.event.ActionEvent;
import mediahand.MediaLoader;
import mediahand.core.MediaHandApp;
import mediahand.repository.base.Database;

public class RootLayoutController {

    public void addDirectory(ActionEvent actionEvent) {
        if (MediaHandApp.chooseBasePath()) {
            MediaHandApp.getMediaHandAppController().fillTableView(Database.getMediaRepository().findAll());
        }
    }

    public void loadNewMediaEntries(ActionEvent actionEvent) {
        new MediaLoader().addAllMedia();
    }
}
