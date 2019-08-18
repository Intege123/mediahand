package mediahand.domain;

import java.util.Objects;

public class SettingsEntry {

    private int id;
    private String profile;
    private int windowWidth;
    private int windowHeight;

    public SettingsEntry(String profile, int windowWidth, int windowHeight) {
        this(0, profile, windowWidth, windowHeight);
    }

    public SettingsEntry(int id, String profile, int windowWidth, int windowHeight) {
        this.id = id;
        this.profile = profile;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProfile() {
        return this.profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public int getWindowWidth() {
        return this.windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public int getWindowHeight() {
        return this.windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SettingsEntry that = (SettingsEntry) o;
        return this.id == that.id &&
                this.windowWidth == that.windowWidth &&
                this.windowHeight == that.windowHeight &&
                Objects.equals(this.profile, that.profile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.profile, this.windowWidth, this.windowHeight);
    }
}
