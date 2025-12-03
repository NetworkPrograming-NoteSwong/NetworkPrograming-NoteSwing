package client.controller;

import client.core.Client;
import client.ui.EditorMainUI;
import global.object.EditMessage;
import global.enums.Mode;

public class EditorController {

    private final Client client;
    private final EditorMainUI ui;
    private final String userId;

    public EditorController(EditorMainUI ui, String userId) {
        this.ui = ui;
        this.userId = userId;
        this.client = new Client(this);   // Client는 Controller에 의존
    }

    public void start() {
        client.connectToServer();
    }

    // ===== UI에서 발생하는 이벤트 =====
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

    // 문서 전체가 바뀐 경우(FULL_SYNC 요청용)
    public void onFullDocumentChanged(String fullText) {
        EditMessage msg = new EditMessage(Mode.FULL_SYNC, userId, fullText);
        msg.offset = 0;
        msg.length = fullText != null ? fullText.length() : 0;

        client.send(msg);
    }


    // 클라이언트 UI 수정하기 위한 메서드
    public void onConnectionStatus(String text) {
        ui.updateConnectionStatus(text);
    }

    // ===== 서버에서 오는 이벤트 =====
    public void onRemoteInsert(int offset, String text) {
        ui.applyInsert(offset, text);
    }

    public void onRemoteDelete(int offset, int length) {
        ui.applyDelete(offset, length);
    }

    public void onRemoteFullSync(String text) {
        ui.setFullDocument(text);
    }

    public void onConnectionLost() {
        ui.updateConnectionStatus("서버 연결 끊김");
    }

}