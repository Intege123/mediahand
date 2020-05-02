package mediahand.controller;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import mediahand.WatchState;
import mediahand.core.MediaHandApp;
import mediahand.domain.MediaEntry;
import mediahand.repository.base.Database;
import mediahand.utils.MessageUtil;
import mediahand.vlc.JavaFXDirectRenderingScene;

public class MediaHandAppController {

    public TableView<MediaEntry> mediaTableView;

    private static ObservableList<MediaEntry> mediaEntries;
    private static FilteredList<MediaEntry> filteredData;

    public TextField titleFilter;
    public ComboBox<String> watchStateFilter;
    public CheckBox showAllCheckbox;
    public CheckBox autoContinueCheckbox;

    public void init() {
        addWatchStateColumn();
        addWatchStateFilter();
        addTitleFieldFilterListener();
        try {
            fillTableView(Database.getMediaRepository().findAll());
        } catch (SQLException throwables) {
            MessageUtil.warningAlert(throwables);
        }
    }

    private void addTitleFieldFilterListener() {
        this.titleFilter.textProperty().addListener((observable, oldValue, newValue) -> MediaHandAppController.filteredData.setPredicate(m -> filter(m, newValue)));
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
            try {
                Database.getMediaRepository().update(mediaEntry);
            } catch (SQLException throwables) {
                MessageUtil.warningAlert(throwables);
            }
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

    public void playEmbeddedMedia() {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.isAvailable()) {
            try {
                File file = MediaHandApp.getMediaLoader().getEpisode(selectedItem.getAbsolutePath(), selectedItem.getCurrentEpisodeNumber());
                JavaFXDirectRenderingScene javaFXDirectRenderingScene = new JavaFXDirectRenderingScene(file, selectedItem);
                String windowTitle = selectedItem.getTitle() + " : Episode " + selectedItem.getCurrentEpisodeNumber();
                javaFXDirectRenderingScene.start(MediaHandApp.getStage(), windowTitle);
            } catch (IOException e) {
                MessageUtil.warningAlert(e);
                changeMediaLocation();
            }
        } else {
            MessageUtil.infoAlert("Play media", "Selected media is not available. Deselect 'Show All' to show only media of connected media directories.");
        }
    }

    private void playMedia() {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.isAvailable()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                File file = MediaHandApp.getMediaLoader().getEpisode(selectedItem.getAbsolutePath(), selectedItem.getCurrentEpisodeNumber());
                try {
                    desktop.open(file);
                } catch (IOException e) {
                    MessageUtil.warningAlert(e);
                }
            } catch (IOException e) {
                MessageUtil.warningAlert(e);
                changeMediaLocation();
            }
        } else {
            MessageUtil.infoAlert("Play media", "Selected media is not available. Deselect 'Show All' to show only media of connected media directories.");
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
            MediaHandApp.getMediaLoader().updateMediaEntry(mediaEntry, Database.getMediaRepository(), selectedItem);
        }
    }

    public void increaseCurrentEpisode() {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getCurrentEpisodeNumber() < selectedItem.getEpisodeNumber()) {
            selectedItem.setCurrentEpisodeNumber(selectedItem.getCurrentEpisodeNumber() + 1);
            try {
                Database.getMediaRepository().update(selectedItem);
            } catch (SQLException throwables) {
                MessageUtil.warningAlert(throwables);
            }
        }
    }

    public void decreaseCurrentEpisode() {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getCurrentEpisodeNumber() > 1) {
            selectedItem.setCurrentEpisodeNumber(selectedItem.getCurrentEpisodeNumber() - 1);
            try {
                Database.getMediaRepository().update(selectedItem);
            } catch (SQLException throwables) {
                MessageUtil.warningAlert(throwables);
            }
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
