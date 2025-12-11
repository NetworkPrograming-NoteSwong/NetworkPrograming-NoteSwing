package client.controller;

import client.core.Client;
import client.ui.EditorMainUI;
import global.enums.Mode;
import global.object.EditMessage;

/**
 * UI와 네트워크 계층의 조율자
 */
public class EditorController {

    private final Client client;
    private final EditorMainUI ui;
    private final String userId;

    public EditorController(EditorMainUI ui, String userId) {
        this.ui = ui;
        this.userId = userId;
        this.client = new Client(this);
    }

    public void start() {
        client.connectToServer();
    }

    // ===== 텍스트: UI -> 서버 =====

    public void onTextInserted(int offset, String text) {
        EditMessage msg = new EditMessage(Mode.INSERT, userId, text);
        msg.offset = offset;
        msg.length = text.length();
        client.send(msg);
    }

    public void onTextDeleted(int offset, int length) {
        EditMessage msg = new EditMessage(Mode.DELETE, userId, null);
        msg.offset = offset;
        msg.length = length;
        client.send(msg);
    }

    // 현재는 클라이언트가 FULL_SYNC를 보낼 일은 거의 없음 (서버 -> 클라 전용으로 사용 중)
    public void onFullDocumentChanged(String fullText) {
        EditMessage msg = new EditMessage(Mode.FULL_SYNC, userId, fullText);
        msg.offset = 0;
        msg.length = fullText != null ? fullText.length() : 0;
        client.send(msg);
    }

    // ===== 이미지: UI -> 서버 =====

    public void onLocalImageInserted(int blockId, int offset,
                                     int width, int height, byte[] data) {
        EditMessage msg = new EditMessage(Mode.IMAGE_INSERT, userId, null);
        msg.blockId = blockId;
        msg.offset = offset;
        msg.length = 1;
        msg.width = width;
        msg.height = height;
        msg.payload = data;
        client.send(msg);
    }

    public void onLocalImageResized(int blockId, int width, int height) {
        EditMessage msg = new EditMessage(Mode.IMAGE_RESIZE, userId, null);
        msg.blockId = blockId;
        msg.width = width;
        msg.height = height;
        client.send(msg);
    }

    public void onLocalImageMoved(int blockId, int newOffset) {
        EditMessage msg = new EditMessage(Mode.IMAGE_MOVE, userId, null);
        msg.blockId = blockId;
        msg.newOffset = newOffset;
        client.send(msg);
    }

    // ===== 서버 -> UI 콜백 =====

    public void onConnectionStatus(String text) {
        ui.updateConnectionStatus(text);
    }

    public void onRemoteInsert(int offset, String text) {
        ui.applyInsert(offset, text);
    }

    public void onRemoteDelete(int offset, int length) {
        ui.applyDelete(offset, length);
    }

    public void onRemoteFullSync(String text) {
        ui.setFullDocument(text);
    }

    public void onRemoteImageInsert(int blockId, int offset,
                                    int width, int height, byte[] data) {
        ui.applyImageInsert(blockId, offset, width, height, data);
    }

    public void onRemoteImageResize(int blockId, int width, int height) {
        ui.applyImageResize(blockId, width, height);
    }

    public void onRemoteImageMove(int blockId, int newOffset) {
        ui.applyImageMove(blockId, newOffset);
    }

    public void onConnectionLost() {
        ui.updateConnectionStatus("서버 연결 끊김");
    }
}
