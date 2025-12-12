package global.object;

import java.io.Serializable;

public class DocumentMeta implements Serializable {
    public String id;
    public String title;
    public long updatedAt;

    public DocumentMeta() {}

    public DocumentMeta(String id, String title, long updatedAt) {
        this.id = id;
        this.title = title;
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return title == null ? "(untitled)" : title;
    }
}
