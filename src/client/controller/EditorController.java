package client.controller;

import client.core.Client;
import client.ui.EditorMainUI;
import global.object.EditMessage;
import global.enums.Mode;

public class EditorController {

    private final Client client;
    private final EditorMainUI ui;

    public EditorController(EditorMainUI ui) {
        this.ui = ui;
        this.client = new Client(this);   // Client는 Controller에 의존
    }

    public void start() {
        client.connectToServer();
    }

    // ===== UI에서 발생하는 이벤트 =====
    public void onTextInserted(int offset, String text) {
        EditMessage msg = new EditMessage(Mode.INSERT, "user1", text);
        msg.offset = offset;
        msg.length = text.length();

        client.send(msg);
    }

    public void onTextDeleted(int offset, int length) {
        EditMessage msg = new EditMessage(Mode.DELETE, "user1", null);
        msg.offset = offset;
        msg.length = length;

        client.send(msg);
    }

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
