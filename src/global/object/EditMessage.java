package global.object;

import global.enums.Mode;

import java.io.Serializable;

public class EditMessage implements Serializable {
    public Mode mode;
    public String userId;
    public String text;

    // ==== 이미지/공통 필드 ====
    public byte[] payload;
    public int blockId;
    public int offset;
    public int length;
    public int width;      // IMAGE_RESIZE, IMAGE_INSERT
    public int height;
    public int newOffset;  // IMAGE_MOVE에서 사용

    public EditMessage(Mode mode, String userId, String text) {
        this.mode = mode;
        this.userId = userId;
        this.text = text;
    }

    @Override
    public String toString() {
        return "[Mode=" + mode +
                ", user=" + userId +
                ", text=" + text +
                ", blockId=" + blockId +
                ", offset=" + offset +
                ", length=" + length +
                ", width=" + width +
                ", height=" + height +
                ", newOffset=" + newOffset + "]";
    }
}
