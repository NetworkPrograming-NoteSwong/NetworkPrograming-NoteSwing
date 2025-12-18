package client.controller;

import client.core.Client;
import client.ui.EditorMainUI;
import client.ui.LobbyUI;
import global.enums.Mode;
import global.object.DocumentMeta;
import global.object.EditMessage;

import java.util.*;

public class EditorController {

    private final Client client;
    private final String userId;

    private LobbyUI lobbyUI;
    private EditorMainUI editorUI;
    private LineLockController lockController;

    private volatile String currentDocId = null;
    private final LinkedHashSet<String> myDocIds = new LinkedHashSet<>();
    private List<DocumentMeta> lastDocList = new ArrayList<>();
    private Integer myLockedLineIndex = null;

    private String lastConnStatus = "서버 연결: 끊김";

    public EditorController(String userId) {
        this.userId = userId;
        this.client = new Client(this);
    }

    public void attachLobby(LobbyUI lobby) {
        this.lobbyUI = lobby;
        if (lobbyUI != null) lobbyUI.updateConnectionStatus(lastConnStatus);
    }

    public void start() {
        client.connectToServer();
    }

    public void onConnected() {
        requestDocList();
    }

    // ===== 로비 -> 워크스페이스 진입 =====
    public void enterWorkspace(Set<String> selectedDocIds, String openDocId) {
        if (selectedDocIds != null) myDocIds.addAll(selectedDocIds);
        if (openDocId != null) myDocIds.add(openDocId);

        openEditorIfNeeded();
        if (openDocId != null) openDocument(openDocId);
    }

    private void openEditorIfNeeded() {
        if (editorUI == null) {
            editorUI = new EditorMainUI();
            editorUI.setController(this);
            editorUI.updateConnectionStatus(lastConnStatus);

            lockController = new LineLockController(client, editorUI, userId);
        }

        if (lobbyUI != null) lobbyUI.setVisible(false);

        editorUI.setDocumentList(filterMyDocs(lastDocList));
    }

    // ===== 문서 명령 =====
    public void requestDocList() {
        client.send(new EditMessage(Mode.DOC_LIST, userId, null));
    }

    public void openDocument(String docId) {
        if (docId == null) return;
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

    // ===== 로컬 편집 이벤트 =====
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

    public void onLocalImageInserted(int blockId, int offset, int w, int h, byte[] data) {
        if (currentDocId == null) return;
        EditMessage msg = new EditMessage(Mode.IMAGE_INSERT, userId, null);
        msg.docId = currentDocId;
        msg.blockId = blockId;
        msg.offset = offset;
        msg.length = 1;
        msg.width = w;
        msg.height = h;
        msg.payload = data;
        client.send(msg);
    }

    public void onLocalImageResized(int blockId, int w, int h) {
        if (currentDocId == null) return;
        EditMessage msg = new EditMessage(Mode.IMAGE_RESIZE, userId, null);
        msg.docId = currentDocId;
        msg.blockId = blockId;
        msg.width = w;
        msg.height = h;
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

    // ===== 라인 락 =====
    public void onLineSwitched(int fromLine, int toLine) {
        if (currentDocId == null || lockController == null) return;

        if (lockController.isLockedByMe(fromLine)) lockController.unlock(currentDocId, fromLine);

        if (lockController.isLockedByOther(toLine) && editorUI != null) {
            editorUI.showLineLockedDialog(toLine);
            return;
        }

        if (!lockController.isLockedByMe(toLine)) lockController.requestLock(currentDocId, toLine);
        myLockedLineIndex = toLine;
    }

    public boolean isLineLockedByOther(int lineIndex) {
        return lockController != null && lockController.isLockedByOther(lineIndex);
    }

    // ===== 서버 이벤트 =====
    public void onConnectionStatus(String text) {
        lastConnStatus = (text == null ? lastConnStatus : text);
        if (lobbyUI != null) lobbyUI.updateConnectionStatus(lastConnStatus);
        if (editorUI != null) editorUI.updateConnectionStatus(lastConnStatus);
    }

    public void onRemoteDocList(List<DocumentMeta> docs) {
        lastDocList = (docs == null) ? new ArrayList<>() : new ArrayList<>(docs);

        if (lobbyUI != null) lobbyUI.setAllDocs(lastDocList);
        if (lobbyUI != null) lobbyUI.setMyDocs(filterMyDocs(lastDocList));
        if (editorUI != null) editorUI.setDocumentList(filterMyDocs(lastDocList));

        // 서버에 남아있는 문서만 유지
        Set<String> alive = new HashSet<>();
        for (DocumentMeta m : lastDocList) if (m != null && m.id != null) alive.add(m.id);
        myDocIds.removeIf(id -> !alive.contains(id));
    }

    public void onRemoteFullSync(String docId, String title, String text) {
        currentDocId = docId;
        if (docId != null) myDocIds.add(docId);
        if (lobbyUI != null) lobbyUI.setMyDocs(filterMyDocs(lastDocList));

        openEditorIfNeeded();
        unlockMyLineIfNeeded();
        if (lockController != null) lockController.resetAllLocks();

        editorUI.onSnapshotFullSync(docId, title, text);
        editorUI.setDocumentList(filterMyDocs(lastDocList));
    }
    public void onRemoteSyncEnd(String docId) { }

    public void onRemoteInsert(String docId, int offset, String text) {
        if (!isCurrent(docId)) return;
        editorUI.applyInsert(offset, text);
    }

    public void onRemoteDelete(String docId, int offset, int length) {
        if (!isCurrent(docId)) return;
        editorUI.applyDelete(offset, length);
    }

    public void onRemoteImageInsert(String docId, int id, int offset, int w, int h, byte[] data) {
        if (!isCurrent(docId)) return;
        editorUI.applyImageInsert(id, offset, w, h, data);
    }

    public void onRemoteImageResize(String docId, int id, int w, int h) {
        if (!isCurrent(docId)) return;
        editorUI.applyImageResize(id, w, h);
    }

    public void onRemoteImageMove(String docId, int id, int newOffset) {
        if (!isCurrent(docId)) return;
        editorUI.applyImageMove(id, newOffset);
    }

    public void onRemoteLock(int lineIndex, String ownerId) {
        if (lockController != null) lockController.onRemoteLock(lineIndex, ownerId);
    }

    public void onRemoteUnlock(int lineIndex, String ownerId) {
        if (lockController != null) lockController.onRemoteUnlock(lineIndex, ownerId);
    }

    public void onRemoteDocDeleted(String deletedDocId) {
        if (deletedDocId == null) return;
        myDocIds.remove(deletedDocId);

        // 내가 보고 있던 문서가 삭제되면 로비로
        if (currentDocId != null && currentDocId.equals(deletedDocId)) {
            backToLobby();
        } else if (editorUI != null) {
            editorUI.setDocumentList(filterMyDocs(lastDocList));
        }
    }

    public void onConnectionLost() {
        onConnectionStatus("서버 연결 끊김");
    }

    // ===== 로비로 돌아가기 =====
    public void backToLobby() {
        sendDocLeave();
        unlockMyLineIfNeeded();

        currentDocId = null;

        if (editorUI != null) {
            editorUI.dispose();
            editorUI = null;
        }
        lockController = null;

        if (lobbyUI != null) {
            lobbyUI.setVisible(true);
            lobbyUI.updateConnectionStatus(lastConnStatus);
            lobbyUI.setAllDocs(lastDocList);
            lobbyUI.setMyDocs(filterMyDocs(lastDocList));
        }
    }

    private void sendDocLeave() {
        if (currentDocId == null) return;
        EditMessage msg = new EditMessage(Mode.DOC_LEAVE, userId, null);
        msg.docId = currentDocId;
        client.send(msg);
    }

    private boolean isCurrent(String docId) {
        return currentDocId != null && currentDocId.equals(docId) && editorUI != null;
    }

    private void unlockMyLineIfNeeded() {
        if (myLockedLineIndex == null || lockController == null || currentDocId == null) {
            myLockedLineIndex = null;
            return;
        }
        if (lockController.isLockedByMe(myLockedLineIndex)) {
            lockController.unlock(currentDocId, myLockedLineIndex);
        }
        myLockedLineIndex = null;
    }

    public void addMyDoc(String docId) {
        if (docId == null) return;
        myDocIds.add(docId);
        if (editorUI != null) editorUI.setDocumentList(filterMyDocs(lastDocList));
        if (lobbyUI != null) lobbyUI.setMyDocs(filterMyDocs(lastDocList));
    }

    public void removeMyDoc(String docId) {
        if (docId == null) return;
        myDocIds.remove(docId);
        if (editorUI != null) editorUI.setDocumentList(filterMyDocs(lastDocList));
        if (lobbyUI != null) lobbyUI.setMyDocs(filterMyDocs(lastDocList));
    }

    private List<DocumentMeta> filterMyDocs(List<DocumentMeta> docs) {
        List<DocumentMeta> r = new ArrayList<>();
        if (docs == null) return r;
        for (DocumentMeta m : docs) {
            if (m == null || m.id == null) continue;
            if (myDocIds.contains(m.id)) r.add(m);
        }
        return r;
    }
}
