package client.controller;

import client.core.Client;
import client.ui.EditorMainUI;
import global.enums.Mode;
import global.object.DocumentMeta;
import global.object.EditMessage;

import java.util.List;

public class EditorController {

    private final Client client;
    private final EditorMainUI ui;
    private final String userId;

    private volatile String currentDocId = null;
    private final LineLockController lockController;
    private Integer myLockedLineIndex = null; //내가 편집하고 있는 라인 인덱스

    public EditorController(EditorMainUI ui, String userId) {
        this.ui = ui;
        this.userId = userId;
        this.client = new Client(this);
        this.lockController = new LineLockController(client, ui, userId);
    }

    public void start() {
        client.connectToServer();
    }

    public void onConnected() {
        requestDocList();
    }

    // ===== 문서 명령 =====
    public void requestDocList() {
        EditMessage msg = new EditMessage(Mode.DOC_LIST, userId, null);
        client.send(msg);
    }

    public void openDocument(String docId) {
        EditMessage msg = new EditMessage(Mode.DOC_OPEN, userId, null);
        msg.docId = docId;
        client.send(msg);
    }

    public void createDocument(String title) {
        EditMessage msg = new EditMessage(Mode.DOC_CREATE, userId, null);
        msg.docTitle = title;
        client.send(msg);
    }

    public void deleteDocument(String docId) {
        EditMessage msg = new EditMessage(Mode.DOC_DELETE, userId, null);
        msg.docId = docId;
        client.send(msg);
    }

    public void onTextInserted(int offset, String text) {
        if (currentDocId == null) return;
        EditMessage msg = new EditMessage(Mode.INSERT, userId, text);
        msg.docId = currentDocId;
        msg.offset = offset;
        msg.length = (text == null ? 0 : text.length());
        client.send(msg);
    }

    public void onTextDeleted(int offset, int length) {
        if (currentDocId == null) return;
        EditMessage msg = new EditMessage(Mode.DELETE, userId, null);
        msg.docId = currentDocId;
        msg.offset = offset;
        msg.length = length;
        client.send(msg);
    }

    public void onFullDocumentChanged(String fullText) {
    }

    public void onLineSwitched(int fromLine, int toLine) {
        if (currentDocId == null) return;

        // 이전 줄 UNLOCK
        if (lockController.isLockedByMe(fromLine)) {
            lockController.unlock(currentDocId, fromLine);
        }

        // 새 줄이 다른 사람이 잠금?
        if (lockController.isLockedByOther(toLine)) {
            ui.showLineLockedDialog(toLine);
            return;
        }

        // 새 줄 LOCK
        if (!lockController.isLockedByMe(toLine)) {
            lockController.requestLock(currentDocId, toLine);
        }

        myLockedLineIndex = toLine;
    }

    public void onLocalImageInserted(int blockId, int offset, int width, int height, byte[] data) {
        if (currentDocId == null) return;
        EditMessage msg = new EditMessage(Mode.IMAGE_INSERT, userId, null);
        msg.docId = currentDocId;
        msg.blockId = blockId;
        msg.offset = offset;
        msg.length = 1;
        msg.width = width;
        msg.height = height;
        msg.payload = data;
        client.send(msg);
    }

    public void onLocalImageResized(int blockId, int width, int height) {
        if (currentDocId == null) return;
        EditMessage msg = new EditMessage(Mode.IMAGE_RESIZE, userId, null);
        msg.docId = currentDocId;
        msg.blockId = blockId;
        msg.width = width;
        msg.height = height;
        client.send(msg);
    }

    public void onLocalImageMoved(int blockId, int newOffset) {
        if (currentDocId == null) return;
        EditMessage msg = new EditMessage(Mode.IMAGE_MOVE, userId, null);
        msg.docId = currentDocId;
        msg.blockId = blockId;
        msg.newOffset = newOffset;
        client.send(msg);
    }

    public void onConnectionStatus(String text) {
        ui.updateConnectionStatus(text);
    }

    public void onRemoteDocList(List<DocumentMeta> docs) {
        ui.setDocumentList(docs);
    }

    public void onRemoteFullSync(String docId, String title, String text) {
        this.currentDocId = docId;

        if (myLockedLineIndex != null && lockController.isLockedByMe(myLockedLineIndex)) {
            lockController.unlock(currentDocId, myLockedLineIndex);
        }

        myLockedLineIndex = null;
        lockController.resetAllLocks();
        ui.onSnapshotFullSync(docId, title, text);
    }

    public void onRemoteSyncEnd(String docId) {
    }

    public void onRemoteInsert(String docId, int offset, String text) {
        if (currentDocId == null || !currentDocId.equals(docId)) return;
        ui.applyInsert(offset, text);
    }



    public void onRemoteDelete(String docId, int offset, int length) {
        if (currentDocId == null || !currentDocId.equals(docId)) return;
        ui.applyDelete(offset, length);
    }


    public void onRemoteImageInsert(String docId, int blockId, int offset, int width, int height, byte[] data) {
        if (currentDocId == null || !currentDocId.equals(docId)) return;
        ui.applyImageInsert(blockId, offset, width, height, data);
    }

    public void onRemoteImageResize(String docId, int blockId, int width, int height) {
        if (currentDocId == null || !currentDocId.equals(docId)) return;
        ui.applyImageResize(blockId, width, height);
    }

    public void onRemoteImageMove(String docId, int blockId, int newOffset) {
        if (currentDocId == null || !currentDocId.equals(docId)) return;
        ui.applyImageMove(blockId, newOffset);
    }

    public void onRemoteLock(int lineIndex, String userId) {
        lockController.onRemoteLock(lineIndex, userId);
    }

    public void onRemoteUnlock(int lineIndex, String userId) {
        lockController.onRemoteUnlock(lineIndex, userId);
    }

    public void onConnectionLost() {
        ui.updateConnectionStatus("서버 연결 끊김");
    }

    public boolean isLineLockedByOther(int lineIndex) {
        return lockController.isLockedByOther(lineIndex);
    }
}