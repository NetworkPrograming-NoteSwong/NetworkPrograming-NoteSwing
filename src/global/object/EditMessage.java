package global.object;

import global.enums.Mode;

import java.io.Serializable;

public class EditMessage implements Serializable {
    public Mode mode; // ENUM!
    public String userId;
    public String text;
    public byte[] payload;
    public int blockId;
    public int offset;
    public int length;

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
                ", length=" + length + "]";
    }

}