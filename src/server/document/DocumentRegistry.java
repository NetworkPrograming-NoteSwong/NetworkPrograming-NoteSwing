package server.document;

import server.storage.DocumentStorage;

import java.util.concurrent.ConcurrentHashMap;

public class DocumentRegistry {

    private final ConcurrentHashMap<String, DocumentRoom> rooms = new ConcurrentHashMap<>();

    public DocumentRoom getIfPresent(String docId) {
        if (docId == null) return null;
        return rooms.get(docId);
    }

    public DocumentRoom getOrCreate(String docId, DocumentStorage storage) {
        if (docId == null) return null;
        return rooms.computeIfAbsent(docId, id -> new DocumentRoom(id, storage));
    }

    public void remove(String docId) {
        if (docId == null) return;
        rooms.remove(docId);
    }
}
