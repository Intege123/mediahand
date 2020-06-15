package mediahand.controller;

import mediahand.core.MediaHandApp;
import mediahand.domain.MediaEntry;
import mediahand.repository.RepositoryFactory;

public class RootLayoutController {

    public void addDirectory() {
        if (MediaHandApp.addBasePath()) {
            MediaHandApp.getMediaHandAppController().fillTableView(RepositoryFactory.getMediaRepository().findAll());
        }
    }

    public void loadNewMediaEntries() {
        MediaHandApp.getMediaLoader().addAllMedia();
        MediaHandApp.getMediaHandAppController().fillTableView(RepositoryFactory.getMediaRepository().findAll());
    }

    public void onRemove() {
        MediaEntry selectedMediaEntry = MediaHandApp.getMediaHandAppController().getSelectedMediaEntry();
        if (selectedMediaEntry != null) {
            RepositoryFactory.getMediaRepository().remove(selectedMediaEntry);
        }
    }

    public void addMedia() {
        MediaHandApp.getMediaLoader().addSingleMedia();
        MediaHandApp.getMediaHandAppController().fillTableView(RepositoryFactory.getMediaRepository().findAll());
    }

}
