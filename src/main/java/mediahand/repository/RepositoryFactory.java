package mediahand.repository;

import mediahand.domain.DirectoryEntry;
import mediahand.domain.MediaEntry;
import mediahand.domain.SettingsEntry;
import mediahand.repository.base.BaseRepository;

public class RepositoryFactory {

    // replace implementation with another one if necessary
    private static final BaseRepository<MediaEntry> MEDIA_REPOSITORY = new MediaRepository();
    private static final BaseRepository<SettingsEntry> SETTINGS_REPOSITORY = new SettingsRepository();
    private static final BaseRepository<DirectoryEntry> BASE_PATH_REPOSITORY = new BasePathRepository();

    public static BaseRepository<MediaEntry> getMediaRepository() {
        return RepositoryFactory.MEDIA_REPOSITORY;
    }

    public static BaseRepository<SettingsEntry> getSettingsRepository() {
        return RepositoryFactory.SETTINGS_REPOSITORY;
    }

    public static BaseRepository<DirectoryEntry> getBasePathRepository() {
        return RepositoryFactory.BASE_PATH_REPOSITORY;
    }

}
