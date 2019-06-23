package mediahand.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import mediahand.domain.MediaEntry;
import mediahand.repository.base.Database;

import java.util.stream.Collectors;

public class MediaHandAppController {

    public ListView listView;

    private ObservableList<MediaEntry> mediaEntries;

    public void playMedia(MouseEvent mouseEvent) {
        System.out.println(this.listView.getSelectionModel().getSelectedItem());
    }

    public void fillListView() {
        this.mediaEntries = FXCollections.observableArrayList(Database.getMediaRepository().findAll());
        this.listView.setItems(FXCollections.observableArrayList(this.mediaEntries.stream()
                .map(MediaEntry::getTitle).collect(Collectors.toList())));
    }

}
