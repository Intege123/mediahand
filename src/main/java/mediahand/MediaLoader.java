package mediahand;

import mediahand.domain.DirectoryEntry;
import mediahand.domain.MediaEntry;
import mediahand.repository.base.Database;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

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
        p = f.toPath();
        try {
            stream = Files.newDirectoryStream(p);
            for (Path dir : stream) {
                if (dir.toFile().isDirectory()) {
                    // recursive execution
                    addMedia(dir.toString());
                } else if (getMediaCount(dir.getParent().toFile()) > 0) {
                    String mediaTitle = dir.getParent().getFileName().toString();
                    int episodeNumber = getMediaCount(dir.getParent().toFile());
                    String mediaType = dir.getParent().getParent().getFileName().toString();
                    String relativePath = dir.getParent().toString().substring(this.basePath.getPath().length());
                    Database.getMediaRepository().create(new MediaEntry(mediaTitle, episodeNumber, mediaType,
                            WatchState.WANT_TO_WATCH, 0, relativePath, 0, null, 0, null, 0, this.basePath));
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getEpisode(final String absolutePath, final int episode) {
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
            throw new IllegalArgumentException();
        }
    }

    private int getMediaCount(final File dir) {
        return Objects.requireNonNull(dir.listFiles((dir1, name) -> isMedia(name))).length;
    }

    private boolean isMedia(final String name) {
        return name.contains(".mkv") || name.contains(".mp4") || name.contains(".flv");
    }

}
