package mediahand.domain;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import mediahand.WatchState;

import java.util.Date;
import java.util.Objects;

public class MediaEntry {

    private int id;
    private String title;
    private int episodeNumber;
    private String mediaType;
    private WatchState watchState;
    private int rating;
    private String path;
    private int currentEpisode;
    private Date added;
    private int episodeLength;
    private Date watchedDate;
    private int watchedCount;
    private DirectoryEntry basePath;
    private boolean available;

    public MediaEntry(final String title, final String path, final DirectoryEntry basePath) {
        this(0, title, 0, null, WatchState.WANT_TO_WATCH, 0, path, 0, null, 0, null, 0, basePath, false);
    }

    public MediaEntry(int id, String title, int episodeNumber, String mediaType, WatchState watchState, int rating, String path, int currentEpisode, Date added, int episodeLength, Date watchedDate, int watchedCount, DirectoryEntry basePath, boolean available) {
        this.id = id;
        this.title = title;
        this.episodeNumber = episodeNumber;
        this.mediaType = mediaType;
        this.watchState = watchState;
        this.rating = rating;
        this.path = path;
        this.currentEpisode = currentEpisode;
        this.added = added;
        this.episodeLength = episodeLength;
        this.watchedDate = watchedDate;
        this.watchedCount = watchedCount;
        this.basePath = basePath;
        this.available = available;
    }

    @Override
    public String toString() {
        return this.title;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getEpisodeNumber() {
        return this.episodeNumber;
    }

    public void setEpisodeNumber(int episodeNumber) {
        this.episodeNumber = episodeNumber;
    }

    public String getMediaType() {
        return this.mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public StringProperty getWatchState() {
        return new SimpleStringProperty(this.watchState.toString());
    }

    public void setWatchState(WatchState watchState) {
        this.watchState = watchState;
    }

    public int getRating() {
        return this.rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getCurrentEpisode() {
        return this.currentEpisode;
    }

    public void setCurrentEpisode(int currentEpisode) {
        this.currentEpisode = currentEpisode;
    }

    public Date getAdded() {
        return this.added;
    }

    public void setAdded(Date added) {
        this.added = added;
    }

    public int getEpisodeLength() {
        return this.episodeLength;
    }

    public void setEpisodeLength(int episodeLength) {
        this.episodeLength = episodeLength;
    }

    public Date getWatchedDate() {
        return this.watchedDate;
    }

    public void setWatchedDate(Date watchedDate) {
        this.watchedDate = watchedDate;
    }

    public int getWatchedCount() {
        return this.watchedCount;
    }

    public void setWatchedCount(int watchedCount) {
        this.watchedCount = watchedCount;
    }

    public DirectoryEntry getBasePath() {
        return this.basePath;
    }

    public void setBasePath(DirectoryEntry basePath) {
        this.basePath = basePath;
    }

    public boolean isAvailable() {
        return this.available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean filterByWatchState(final String watchState) {
        return watchState.equals("ALL") || watchState.equals(this.watchState.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaEntry that = (MediaEntry) o;
        return this.title.equals(that.title) &&
                this.path.equals(that.path) &&
                this.basePath.equals(that.basePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.title, this.path, this.basePath);
    }
}
