package server.document;

import global.enums.Mode;
import global.object.DocumentState;
import global.object.EditMessage;
import server.core.ClientHandler;
import server.storage.DocumentStorage;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DocumentRoom {

    private final String docId;
    private final DocumentManager manager = new DocumentManager();
    private final DocumentStorage storage;

    private final Set<ClientHandler> members = ConcurrentHashMap.newKeySet();
    private volatile boolean loaded = false;

    private volatile long lastSaveMs = 0;
    private int dirtyEdits = 0;

    public DocumentRoom(String docId, DocumentStorage storage) {
        this.docId = docId;
        this.storage = storage;
    }

    public String getDocId() { return docId; }

    public synchronized void loadIfNeeded() {
        if (loaded) return;

        storage.ensureExists(docId);
        DocumentState state = storage.load(docId);
        if (state != null) manager.loadState(state);

        loaded = true;
    }

    public void join(ClientHandler h) {
        members.add(h);
        h.setCurrentDocId(docId);
    }

    public void leave(ClientHandler h) {
        members.remove(h);
    }

    public void sendSnapshotTo(ClientHandler h) {
        loadIfNeeded();

        EditMessage full = new EditMessage(Mode.FULL_SYNC, "server", manager.getDocument());
        full.docId = docId;
        full.docTitle = storage.getTitle(docId);
        full.offset = 0;
        full.length = (full.text == null) ? 0 : full.text.length();
        h.send(full);

        for (EditMessage img : manager.buildFullImageSyncMessages(docId, "server")) {
            h.send(img);
        }

        EditMessage end = new EditMessage(Mode.SYNC_END, "server", null);
        end.docId = docId;
        h.send(end);
    }

    public void applyAndBroadcast(EditMessage msg, ClientHandler sender) {
        loadIfNeeded();
        manager.apply(msg);

        for (ClientHandler h : members) {
            if (h == sender) continue;
            h.send(msg);
        }

        autosaveMaybe();
    }

    private synchronized void autosaveMaybe() {
        dirtyEdits++;
        long now = System.currentTimeMillis();

        if (dirtyEdits >= 15 || now - lastSaveMs >= 1200) {
            dirtyEdits = 0;
            lastSaveMs = now;
            storage.save(docId, manager.createState());
        }
    }

    public synchronized void saveNow() {
        loadIfNeeded();
        storage.save(docId, manager.createState());
        dirtyEdits = 0;
        lastSaveMs = System.currentTimeMillis();
    }
}
