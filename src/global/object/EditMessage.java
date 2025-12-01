// src/global/object/EditMessage.java
package global.object;

import global.enums.Mode;

import java.io.Serializable;

public class EditMessage implements Serializable {
    public Mode mode;      // ENUM!
    public String userId;

    // 텍스트 관련
    public String text;

    // 바이너리 payload (이미지 등)
    public byte[] payload;

    // 블록 단위(이미지 등) 식별자
    public int blockId;

    // 오프셋 및 길이 (텍스트/이미지 공용)
    public int offset;
    public int length;

    // 이미지 전용 속성
    public int width;
    public int height;

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
                ", height=" + height + "]";
    }
}