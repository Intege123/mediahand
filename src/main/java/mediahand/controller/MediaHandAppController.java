package mediahand.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import mediahand.WatchState;
import mediahand.core.MediaHandApp;
import mediahand.domain.MediaEntry;
import mediahand.repository.base.Database;
import mediahand.vlc.JavaFXDirectRenderingScene;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class MediaHandAppController {

    public TableView mediaTableView;
    public CheckBox showAllCheckbox;

    private static ObservableList<MediaEntry> mediaEntries;
    private static FilteredList<MediaEntry> filteredData;

    public void init() {
        addWatchStateColumn();
        fillTableView(Database.getMediaRepository().findAll());
    }

    public void onPlayEnter(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            playMedia();
        }
    }

    public void onPlayButton(ActionEvent actionEvent) {
        playEmbeddedMedia();
    }

    private void addWatchStateColumn() {
        TableColumn<MediaEntry, String> watchStateColumn = new TableColumn<>("Watch State");
        watchStateColumn.prefWidthProperty().bind(this.mediaTableView.widthProperty().multiply(0.15).subtract(22));
        watchStateColumn.setMaxWidth(Integer.MAX_VALUE);
        watchStateColumn.setCellValueFactory(cellData -> cellData.getValue().getWatchState());
        watchStateColumn.setCellFactory(ComboBoxTableCell.forTableColumn(new String[]{WatchState.WANT_TO_WATCH.toString(), WatchState.DOWNLOADING.toString(), WatchState.WATCHED.toString(), WatchState.WATCHING.toString(), WatchState.REWATCHING.toString()}));
        watchStateColumn.setOnEditCommit(event -> {
            MediaEntry mediaEntry = event.getRowValue();
            mediaEntry.setWatchState(WatchState.valueOf(event.getNewValue()));
            Database.getMediaRepository().update(mediaEntry);
        });

        this.mediaTableView.setEditable(true);

        this.mediaTableView.getColumns().add(watchStateColumn);
    }

    public void fillTableView(List<MediaEntry> mediaEntries) {
        MediaHandAppController.mediaEntries = FXCollections.observableArrayList(mediaEntries);

        MediaHandAppController.filteredData = new FilteredList<>(MediaHandAppController.mediaEntries);
        setFilteredDataPredicate();

        this.mediaTableView.setItems(MediaHandAppController.filteredData);
    }

    private void playEmbeddedMedia() {
        MediaEntry selectedItem = (MediaEntry) this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.isAvailable()) {
            try {
                File file = MediaHandApp.getMediaLoader().getEpisode(selectedItem.getBasePath().getPath() + selectedItem.getPath(), selectedItem.getCurrentEpisode());
                JavaFXDirectRenderingScene javaFXDirectRenderingScene = new JavaFXDirectRenderingScene(file);
                javaFXDirectRenderingScene.start(MediaHandApp.getStage());
            } catch (IOException e) {
                changeMediaLocation();
            }
        }
    }

    private void playMedia() {
        MediaEntry selectedItem = (MediaEntry) this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.isAvailable()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                File file = MediaHandApp.getMediaLoader().getEpisode(selectedItem.getBasePath().getPath() + selectedItem.getPath(), selectedItem.getCurrentEpisode());
                try {
                    desktop.open(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                changeMediaLocation();
            }
        }
    }

    private void changeMediaLocation() {
        Optional<File> directory = MediaHandApp.chooseMediaDirectory();
        if (directory.isPresent()) {
            MediaEntry updatedMediaEntry = MediaHandApp.getMediaLoader().createTempMediaEntry(directory.get().toPath());
            updateMedia(updatedMediaEntry);
        }
    }

    private void updateMedia(final MediaEntry mediaEntry) {
        MediaEntry selectedItem = (MediaEntry) this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            MediaHandApp.getMediaLoader().updateMediaEntryPath(mediaEntry, Database.getMediaRepository(), selectedItem);
            triggerMediaEntryUpdate(selectedItem);
        }
    }

    public void increaseCurrentEpisode(ActionEvent actionEvent) {
        MediaEntry selectedItem = (MediaEntry) this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getCurrentEpisode() < selectedItem.getEpisodeNumber()) {
            selectedItem.setCurrentEpisode(selectedItem.getCurrentEpisode() + 1);
            Database.getMediaRepository().update(selectedItem);
            triggerMediaEntryUpdate(selectedItem);
        }
    }

    public void decreaseCurrentEpisode(ActionEvent actionEvent) {
        MediaEntry selectedItem = (MediaEntry) this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getCurrentEpisode() > 1) {
            selectedItem.setCurrentEpisode(selectedItem.getCurrentEpisode() - 1);
            Database.getMediaRepository().update(selectedItem);
            triggerMediaEntryUpdate(selectedItem);
        }
    }

    public static void triggerMediaEntryUpdate(MediaEntry mediaEntry) {
        MediaHandAppController.mediaEntries.set(MediaHandAppController.mediaEntries.indexOf(mediaEntry), mediaEntry);
    }

    public void onShowAll(ActionEvent actionEvent) {
        setFilteredDataPredicate();
    }

    public static ObservableList<MediaEntry> getMediaEntries() {
        return MediaHandAppController.mediaEntries;
    }

    private void setFilteredDataPredicate() {
        if (this.showAllCheckbox.isSelected()) {
            MediaHandAppController.filteredData.setPredicate(m -> true);
        } else {
            MediaHandAppController.filteredData.setPredicate(MediaEntry::isAvailable);
        }
    }
}
