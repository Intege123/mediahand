package mediahand.domain;

public class DirectoryEntry {

    private int id;
    private String path;

    public DirectoryEntry(String path) {
        this.path = path;
    }

    public DirectoryEntry(int id, String path) {
        this.id = id;
        this.path = path;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
