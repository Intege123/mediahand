package mediahand;

import javafx.collections.transformation.FilteredList;
import mediahand.controller.MediaHandAppController;
import mediahand.domain.DirectoryEntry;
import mediahand.domain.MediaEntry;
import mediahand.repository.MediaRepository;
import mediahand.repository.base.Database;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Loads media into the database.
 *
 * @author Lueko
 */
public class MediaLoader {

    private DirectoryEntry basePath;

    /**
     * Adds all media of every directory path in dirTable into the mediaTable.
     */
    public void addAllMedia() {
        List<DirectoryEntry> basePaths = Database.getBasePathRepository().findAll();

        for (DirectoryEntry path : basePaths) {
            addMedia(path);
        }

    }

    public void addMedia(final DirectoryEntry basePath) {
        this.basePath = basePath;
        addMedia(basePath.getPath());
    }

    /**
     * Adds all media in a directory into mediaTable.
     *
     * @param path Directory with media inside.
     */
    private void addMedia(final String path) {
        File f;
        Path p;
        DirectoryStream<Path> stream;

        f = new File(path);
        if (f.exists()) {
            p = f.toPath();
            try {
                stream = Files.newDirectoryStream(p);
                for (Path dir : stream) {
                    if (dir.toFile().isDirectory()) {
                        // recursive execution
                        addMedia(dir.toString());
                    } else if (getMediaCount(dir.getParent().toFile()) > 0) {
                        MediaEntry newMediaEntry = createTempMediaEntry(dir.getParent(), this.basePath);
                        addSingleMedia(newMediaEntry);
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Add a new {@link MediaEntry} to the database if a {@link MediaEntry} with the same name in that base path does not exist. Else update
     * the {@link MediaEntry}'s episode number.
     *
     * @param newMediaEntry the {@link MediaEntry} to add
     */
    private void addSingleMedia(final MediaEntry newMediaEntry) {
        MediaRepository mediaRepository = Database.getMediaRepository();
        FilteredList<MediaEntry> mediaEntryFilteredList = MediaHandAppController.getMediaEntries()
                .filtered(m -> m.getTitle().equals(newMediaEntry.getTitle()));
        if (mediaEntryFilteredList.isEmpty()) {
            mediaRepository.create(newMediaEntry);
        } else {
            MediaEntry mediaEntry = mediaEntryFilteredList.get(0);
            updateMediaEntryEpisodes(newMediaEntry, mediaRepository, mediaEntry);
        }
    }

    public MediaEntry createTempMediaEntry(final Path mediaDirectory) {
        List<DirectoryEntry> allBasePaths = Database.getBasePathRepository().findAll();
        String basePath = mediaDirectory.getParent().getParent().toString();
        Optional<DirectoryEntry> optionalBasePath = allBasePaths.stream().filter(directoryEntry -> directoryEntry.getPath().equals(basePath)).findFirst();
        if (optionalBasePath.isEmpty()) {
            optionalBasePath = Optional.of(Database.getBasePathRepository().create(new DirectoryEntry(basePath)));
        }
        String mediaTitle = mediaDirectory.getFileName().toString();
        int episodeNumber = getMediaCount(mediaDirectory.toFile());
        String mediaType = mediaDirectory.getParent().getFileName().toString();
        String relativePath = mediaDirectory.toString().substring(optionalBasePath.get().getPath().length());
        return new MediaEntry(0, mediaTitle, episodeNumber, mediaType,
                WatchState.WANT_TO_WATCH, 0, relativePath, 0, null, 0, null, 0, optionalBasePath.get(), false);
    }

    private MediaEntry createTempMediaEntry(final Path mediaDirectory, final DirectoryEntry basePath) {
        String mediaTitle = mediaDirectory.getFileName().toString();
        int episodeNumber = getMediaCount(mediaDirectory.toFile());
        String mediaType = mediaDirectory.getParent().getFileName().toString();
        String relativePath = mediaDirectory.toString().substring(this.basePath.getPath().length());
        return new MediaEntry(0, mediaTitle, episodeNumber, mediaType,
                WatchState.WANT_TO_WATCH, 0, relativePath, 0, null, 0, null, 0, basePath, false);
    }

    private void updateMediaEntryEpisodes(final MediaEntry newMediaEntry, final MediaRepository mediaRepository, final MediaEntry mediaEntry) {
        mediaEntry.setEpisodeNumber(newMediaEntry.getEpisodeNumber());
        if (mediaEntry.getCurrentEpisode() > mediaEntry.getEpisodeNumber()) {
            mediaEntry.setCurrentEpisode(mediaEntry.getEpisodeNumber());
        }
        mediaRepository.update(mediaEntry);
        MediaHandAppController.triggerMediaEntryUpdate(mediaEntry);
    }

    public void updateMediaEntryPath(final MediaEntry newMediaEntry, final MediaRepository mediaRepository, final MediaEntry mediaEntry) {
        mediaEntry.setTitle(newMediaEntry.getTitle());
        mediaEntry.setBasePath(newMediaEntry.getBasePath());
        mediaEntry.setPath(newMediaEntry.getPath());
        mediaEntry.setEpisodeNumber(newMediaEntry.getEpisodeNumber());
        mediaEntry.setMediaType(newMediaEntry.getMediaType());
        if (mediaEntry.getCurrentEpisode() > mediaEntry.getEpisodeNumber()) {
            mediaEntry.setCurrentEpisode(mediaEntry.getEpisodeNumber());
        }
        mediaRepository.update(mediaEntry);
        MediaHandAppController.triggerMediaEntryUpdate(mediaEntry);
    }

    public File getEpisode(final String absolutePath, final int episode) throws IOException {
        File dir = new File(absolutePath);
        if (dir.isDirectory()) {
            File[] files = dir.listFiles((dir2, name) -> isMedia(name));
            if (files != null && files.length > episode - 1) {
                return files[episode - 1];
            } else {
                System.err.println("Episode " + episode + " does not exist in \"" + absolutePath + "\".");
                throw new IllegalArgumentException();
            }
        } else {
            System.err.println("\"" + absolutePath + "\" does not exist or is not a directory.");
            throw new IOException("\"" + absolutePath + "\" does not exist or is not a directory.");
        }
    }

    private int getMediaCount(final File dir) {
        return Objects.requireNonNull(dir.listFiles((dir1, name) -> isMedia(name))).length;
    }

    private boolean isMedia(final String name) {
        return name.contains(".mkv") || name.contains(".mp4") || name.contains(".flv");
    }

}
