package global.object;

import global.enums.Mode;
import java.io.Serializable;
import java.util.List;

public class EditMessage implements Serializable {
    public Mode mode;
    public String userId;
    public String text;

    // ===== 문서 라우팅 =====
    public String docId;
    public String docTitle;
    public List<DocumentMeta> docs;

    // ===== 이미지/공통 필드 =====
    public byte[] payload;
    public int blockId;
    public int offset;
    public int length;
    public int width;
    public int height;
    public int newOffset;

    public EditMessage(Mode mode, String userId, String text) {
        this.mode = mode;
        this.userId = userId;
        this.text = text;
    }

    @Override
    public String toString() {
        return "[Mode=" + mode +
                ", user=" + userId +
                ", docId=" + docId +
                ", text=" + text +
                ", blockId=" + blockId +
                ", offset=" + offset +
                ", length=" + length +
                ", width=" + width +
                ", height=" + height +
                ", newOffset=" + newOffset + "]";
    }
}
