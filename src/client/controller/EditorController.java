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

    //다른 클라이언트가 커서 이동한 경우
    public void onCursorMoved(int offset, int length) {
        // 너무 자주 보내는게 부담이면 나중에 rate limit 플래그 추가 예정
        EditMessage msg = new EditMessage(Mode.CURSOR, userId, null);
        msg.offset = offset;
        msg.length = length;
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

    public void onRemoteCursor(String userId, int offset, int length) {
        ui.showRemoteCursor(userId, offset, length);
    }

    public void onRemoteLock(int lineIndex, String ownerUserId) {
        // 1) 내가 잠근 줄이면 UI에 굳이 "다른 사람 잠금"으로 처리할 필요 없음
        if (this.userId.equals(ownerUserId)) {
            return;
        }
        // 2) 다른 사람이 잠근 줄만 UI에 전달
        ui.lockLine(lineIndex, ownerUserId);
    }

    public void onRemoteUnlock(int lineIndex, String ownerUserId) {
        // 내가 잠근 줄 해제라면 지금 단계에서는 아무 UI 행동 안 해도 되고,
        // 다른 사람이 잠근 줄 해제라면, 더 이상 막지 않도록 UI에 알려야 함.
        if (this.userId.equals(ownerUserId)) {
            return;
        }
        ui.unlockLine(lineIndex);
    }

    public void onConnectionLost() {
        ui.updateConnectionStatus("서버 연결 끊김");
    }

}
