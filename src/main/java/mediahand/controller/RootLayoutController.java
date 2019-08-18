package mediahand.controller;

import javafx.event.ActionEvent;
import mediahand.core.MediaHandApp;
import mediahand.domain.MediaEntry;
import mediahand.repository.base.Database;

public class RootLayoutController {

    public void addDirectory(ActionEvent actionEvent) {
        if (MediaHandApp.chooseBasePath()) {
            MediaHandApp.getMediaHandAppController().fillTableView(Database.getMediaRepository().findAll());
        }
    }

    public void loadNewMediaEntries(ActionEvent actionEvent) {
        MediaHandApp.getMediaLoader().addAllMedia();
        MediaHandApp.getMediaHandAppController().fillTableView(Database.getMediaRepository().findAll());
    }

    public void onDelete(ActionEvent actionEvent) {
        MediaEntry selectedMediaEntry = MediaHandApp.getMediaHandAppController().getSelectedMediaEntry();
        if (selectedMediaEntry != null) {
            Database.getMediaRepository().remove(selectedMediaEntry);
            MediaHandAppController.getMediaEntries().remove(selectedMediaEntry);
        }
    }
}
