package mediahand.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
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

    public TableView<MediaEntry> mediaTableView;
    public CheckBox showAllCheckbox;

    private static ObservableList<MediaEntry> mediaEntries;
    private static FilteredList<MediaEntry> filteredData;
    public ComboBox<String> watchStateFilter;
    public TextField titleFilter;

    public void init() {
        addWatchStateColumn();
        addWatchStateFilter();
        addTitleFieldFilterListener();
        fillTableView(Database.getMediaRepository().findAll());
    }

    private void addTitleFieldFilterListener() {
        this.titleFilter.textProperty().addListener((observable, oldValue, newValue) -> {
            MediaHandAppController.filteredData.setPredicate(m -> filter(m, newValue));
        });
    }

    private void addWatchStateFilter() {
        this.watchStateFilter.setItems(FXCollections.observableArrayList("ALL", WatchState.WANT_TO_WATCH.toString(), WatchState.DOWNLOADING.toString(), WatchState.WATCHED.toString(), WatchState.WATCHING.toString(), WatchState.REWATCHING.toString()));
        this.watchStateFilter.getSelectionModel().selectFirst();
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

        MediaHandAppController.filteredData = new FilteredList<>(MediaHandAppController.mediaEntries, this::filter);

        SortedList<MediaEntry> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(this.mediaTableView.comparatorProperty());

        this.mediaTableView.setItems(sortedData);

        TableColumn<MediaEntry, ?> mediaEntryTableTitleColumn = this.mediaTableView.getColumns().get(0);
        mediaEntryTableTitleColumn.setSortType(TableColumn.SortType.ASCENDING);
        this.mediaTableView.getSortOrder().add(mediaEntryTableTitleColumn);
    }

    private void playEmbeddedMedia() {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.isAvailable()) {
            try {
                File file = MediaHandApp.getMediaLoader().getEpisode(selectedItem.getBasePath().getPath() + selectedItem.getPath(), selectedItem.getCurrentEpisode());
                JavaFXDirectRenderingScene javaFXDirectRenderingScene = new JavaFXDirectRenderingScene(file);
                javaFXDirectRenderingScene.start(MediaHandApp.getStage(), selectedItem.getTitle() + " : Episode " + selectedItem.getCurrentEpisode());
            } catch (IOException e) {
                changeMediaLocation();
            }
        }
    }

    private void playMedia() {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
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
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            MediaHandApp.getMediaLoader().updateMediaEntryPath(mediaEntry, Database.getMediaRepository(), selectedItem);
            triggerMediaEntryUpdate(selectedItem);
        }
    }

    public void increaseCurrentEpisode(ActionEvent actionEvent) {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getCurrentEpisode() < selectedItem.getEpisodeNumber()) {
            selectedItem.setCurrentEpisode(selectedItem.getCurrentEpisode() + 1);
            Database.getMediaRepository().update(selectedItem);
            triggerMediaEntryUpdate(selectedItem);
        }
    }

    public void decreaseCurrentEpisode(ActionEvent actionEvent) {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getCurrentEpisode() > 1) {
            selectedItem.setCurrentEpisode(selectedItem.getCurrentEpisode() - 1);
            Database.getMediaRepository().update(selectedItem);
            triggerMediaEntryUpdate(selectedItem);
        }
    }

    public static void triggerMediaEntryUpdate(MediaEntry mediaEntry) {
        MediaHandAppController.mediaEntries.set(MediaHandAppController.mediaEntries.indexOf(mediaEntry), mediaEntry);
    }

    public static ObservableList<MediaEntry> getMediaEntries() {
        return MediaHandAppController.mediaEntries;
    }

    public MediaEntry getSelectedMediaEntry() {
        return this.mediaTableView.getSelectionModel().getSelectedItem();
    }

    public void onFilter(ActionEvent actionEvent) {
        MediaHandAppController.filteredData.setPredicate(this::filter);
    }

    private boolean filter(final MediaEntry mediaEntry) {
        return filter(mediaEntry, this.titleFilter.textProperty().getValue());
    }

    private boolean filter(final MediaEntry mediaEntry, final String textFilter) {
        if (this.showAllCheckbox.isSelected() || mediaEntry.isAvailable()) {
            if (mediaEntry.filterByWatchState(this.watchStateFilter.getSelectionModel().getSelectedItem())) {
                if (textFilter == null || textFilter.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = textFilter.toLowerCase();

                return mediaEntry.getTitle().toLowerCase().contains(lowerCaseFilter);
            }
        }
        return false;
    }

}
