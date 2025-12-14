package client.controller;

import client.core.Client;
import client.ui.EditorMainUI;
import global.enums.Mode;
import global.object.EditMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LineLockController {

    private final Client client;
    private final EditorMainUI ui;
    private final String userId;

    // lineIndex -> ownerUserId
    private final Map<Integer, String> ownerByLine = new ConcurrentHashMap<>();

    public LineLockController(Client client, EditorMainUI ui, String userId) {
        this.client = client;
        this.ui = ui;
        this.userId = userId;
    }

    // ===== 송신 =====
    public void requestLock(String docId, int lineIndex) {
        if (docId == null) return;
        EditMessage msg = new EditMessage(Mode.LOCK, userId, null);
        msg.docId = docId;
        msg.blockId = lineIndex;
        msg.userId = userId;
        client.send(msg);
    }

    public void unlock(String docId, int lineIndex) {
        if (docId == null) return;
        EditMessage msg = new EditMessage(Mode.UNLOCK, userId, null);
        msg.docId = docId;
        msg.blockId = lineIndex;
        msg.userId = userId;
        client.send(msg);
    }

    // ===== 조회 =====
    public boolean isLockedByOther(int lineIndex) {
        String owner = ownerByLine.get(lineIndex);
        return owner != null && !owner.equals(userId);
    }

    public boolean isLockedByMe(int lineIndex) {
        String owner = ownerByLine.get(lineIndex);
        return userId.equals(owner);
    }

    // ===== 서버 이벤트 반영 =====
    public void onRemoteLock(int lineIndex, String ownerId) {
        if (ownerId == null) return;
        ownerByLine.put(lineIndex, ownerId);

        // 내가 잡은 락이면: 상태만 기록, UI 하이라이트 X
        if (userId.equals(ownerId)) return;

        // 다른 사용자 락이면: 하이라이트
        ui.lockLine(lineIndex, ownerId);
    }

    public void onRemoteUnlock(int lineIndex, String ownerId) {
        ownerByLine.remove(lineIndex);

        // 하이라이트만 제거 (내 락이든 다른 사람 락이든)
        ui.unlockLine(lineIndex, ownerId);
    }

    public void resetAllLocks() {
        ownerByLine.clear();
        ui.clearAllLineHighlights();
    }
}

