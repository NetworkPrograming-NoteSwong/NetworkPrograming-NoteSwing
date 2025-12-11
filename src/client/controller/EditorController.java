package client.controller;

import client.core.Client;
import client.ui.EditorMainUI;
import global.object.EditMessage;
import global.enums.Mode;

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

    // ===== UI -> 서버 : 텍스트 =====
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

    // (현재는 클라이언트에서 FULL_SYNC 전송 사용 안 함)
    public void onFullDocumentChanged(String fullText) {
        EditMessage msg = new EditMessage(Mode.FULL_SYNC, userId, fullText);
        msg.offset = 0;
        msg.length = fullText != null ? fullText.length() : 0;
        client.send(msg);
    }

    // ===== UI -> 서버 : 이미지 =====
    public void onImageInserted(int offset, byte[] imageBytes) {
        EditMessage msg = new EditMessage(Mode.IMAGE_INSERT, userId, null);
        msg.offset = offset;
        msg.length = 1;
        msg.payload = imageBytes;
        // 간단히 타임스탬프로 blockId 생성
        msg.blockId = (int) (System.currentTimeMillis() & 0x7fffffff);

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

    public void onRemoteImageInsert(int offset, int blockId, byte[] payload) {
        ui.applyImageInsert(offset, blockId, payload);
    }

    public void onConnectionLost() {
        ui.updateConnectionStatus("서버 연결 끊김");
    }
}
