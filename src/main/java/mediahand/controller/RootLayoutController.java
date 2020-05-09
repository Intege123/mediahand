package mediahand.controller;

import mediahand.core.MediaHandApp;
import mediahand.domain.MediaEntry;
import mediahand.repository.base.Database;

public class RootLayoutController {

    public void addDirectory() {
        if (MediaHandApp.addBasePath()) {
            MediaHandApp.getMediaHandAppController().fillTableView(Database.getMediaRepository().findAll());
        }
    }

    public void loadNewMediaEntries() {
        MediaHandApp.getMediaLoader().addAllMedia();
        MediaHandApp.getMediaHandAppController().fillTableView(Database.getMediaRepository().findAll());
    }

    public void onRemove() {
        MediaEntry selectedMediaEntry = MediaHandApp.getMediaHandAppController().getSelectedMediaEntry();
        if (selectedMediaEntry != null) {
            Database.getMediaRepository().remove(selectedMediaEntry);
        }
    }

    public void addMedia() {
        MediaHandApp.getMediaLoader().addSingleMedia();
        MediaHandApp.getMediaHandAppController().fillTableView(Database.getMediaRepository().findAll());
    }

}
