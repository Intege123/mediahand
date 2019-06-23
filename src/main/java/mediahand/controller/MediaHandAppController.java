package mediahand.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import mediahand.core.MediaHandApp;
import mediahand.domain.MediaEntry;
import mediahand.repository.base.Database;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MediaHandAppController {

    public TableView mediaTableView;

    private ObservableList<MediaEntry> mediaEntries;

    public void onPlayEnter(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            playMedia();
        }
    }

    public void onPlayButton(ActionEvent actionEvent) {
        playMedia();
    }

    public void fillTableView() {
        this.mediaEntries = FXCollections.observableArrayList(Database.getMediaRepository().findAll());
        this.mediaTableView.setItems(this.mediaEntries);
    }

    private void playMedia() {
        MediaEntry selectedItem = (MediaEntry) this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            Desktop desktop = Desktop.getDesktop();
            File file = MediaHandApp.getMediaLoader().getEpisode(selectedItem.getBasePath().getPath() + selectedItem.getPath(), selectedItem.getCurrentEpisode());
            try {
                desktop.open(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void increaseCurrentEpisode(ActionEvent actionEvent) {
        MediaEntry selectedItem = (MediaEntry) this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getCurrentEpisode() + 1 < selectedItem.getEpisodeNumber()) {
            selectedItem.setCurrentEpisode(selectedItem.getCurrentEpisode() + 1);
            Database.getMediaRepository().update(selectedItem);
            this.mediaEntries.set(this.mediaEntries.indexOf(selectedItem), selectedItem);
        }
    }

    public void decreaseCurrentEpisode(ActionEvent actionEvent) {
        MediaEntry selectedItem = (MediaEntry) this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getCurrentEpisode() > 1) {
            selectedItem.setCurrentEpisode(selectedItem.getCurrentEpisode() - 1);
            Database.getMediaRepository().update(selectedItem);
            this.mediaEntries.set(this.mediaEntries.indexOf(selectedItem), selectedItem);
        }
    }
}
