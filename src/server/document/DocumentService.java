package server.document;

import global.object.DocumentMeta;
import global.object.EditMessage;
import server.core.ClientHandler;
import server.storage.DocumentStorage;

import java.util.List;

public class DocumentService {

    private final DocumentStorage storage;
    private final DocumentRegistry registry = new DocumentRegistry();

    public DocumentService(DocumentStorage storage) {
        this.storage = storage;
    }

    public List<DocumentMeta> listDocs() {
        return storage.listMetas();
    }

    public DocumentMeta create(String title) {
        return storage.create(title);
    }

    public boolean delete(String docId) {
        registry.remove(docId);
        return storage.delete(docId);
    }

    public void open(String docId, ClientHandler h) {
        if (docId == null || h == null) return;

        leave(h);

        DocumentRoom room = registry.getOrCreate(docId, storage);
        room.join(h);
        room.sendSnapshotTo(h);
    }

    public void leave(ClientHandler h) {
        if (h == null) return;

        String old = h.getCurrentDocId();
        if (old == null) return;

        DocumentRoom room = registry.getIfPresent(old);
        if (room != null) room.leave(h);

        h.setCurrentDocId(null);
    }

    public void applyEdit(EditMessage msg, ClientHandler sender) {
        if (msg == null || msg.docId == null || sender == null) return;

        String cur = sender.getCurrentDocId();
        if (cur == null || !cur.equals(msg.docId)) return;

        DocumentRoom room = registry.getOrCreate(msg.docId, storage);
        room.applyAndBroadcast(msg, sender);
    }
}
