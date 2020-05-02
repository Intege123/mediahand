package mediahand.controller;

import java.sql.SQLException;

import mediahand.core.MediaHandApp;
import mediahand.domain.MediaEntry;
import mediahand.repository.base.Database;
import mediahand.utils.MessageUtil;

public class RootLayoutController {

    public void addDirectory() {
        if (MediaHandApp.addBasePath()) {
            try {
                MediaHandApp.getMediaHandAppController().fillTableView(Database.getMediaRepository().findAll());
            } catch (SQLException throwables) {
                MessageUtil.warningAlert(throwables);
            }
        }
    }

    public void loadNewMediaEntries() {
        MediaHandApp.getMediaLoader().addAllMedia();
        try {
            MediaHandApp.getMediaHandAppController().fillTableView(Database.getMediaRepository().findAll());
        } catch (SQLException throwables) {
            MessageUtil.warningAlert(throwables);
        }
    }

    public void onRemove() {
        MediaEntry selectedMediaEntry = MediaHandApp.getMediaHandAppController().getSelectedMediaEntry();
        if (selectedMediaEntry != null) {
            try {
                Database.getMediaRepository().remove(selectedMediaEntry);
            } catch (SQLException throwables) {
                MessageUtil.warningAlert(throwables);
            }
        }
    }

    public void addMedia() {
        MediaHandApp.getMediaLoader().addSingleMedia();
        try {
            MediaHandApp.getMediaHandAppController().fillTableView(Database.getMediaRepository().findAll());
        } catch (SQLException throwables) {
            MessageUtil.warningAlert(throwables);
        }
    }

}
