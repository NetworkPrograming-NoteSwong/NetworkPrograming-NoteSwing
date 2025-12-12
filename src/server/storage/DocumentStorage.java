package server.storage;

import global.object.DocumentMeta;
import global.object.DocumentState;

import java.io.File;
import java.util.*;

public class DocumentStorage {

    private static final String STATE_FILE = "state.bin";
    private static final String META_FILE  = "meta.bin";

    private final File rootDir;
    private final DocumentFileStore store = new DocumentFileStore();

    public DocumentStorage() {
        this("data");
    }

    public DocumentStorage(String rootPath) {
        this.rootDir = new File(rootPath == null ? "data" : rootPath);
        if (!rootDir.exists()) rootDir.mkdirs();
    }

    private File docDir(String docId) {
        return new File(rootDir, docId);
    }

    private File stateFile(String docId) {
        return new File(docDir(docId), STATE_FILE);
    }

    private File metaFile(String docId) {
        return new File(docDir(docId), META_FILE);
    }

    public synchronized void ensureExists(String docId) {
        if (docId == null || docId.isBlank()) return;

        if (!rootDir.exists()) rootDir.mkdirs();

        File dir = docDir(docId);
        if (!dir.exists()) dir.mkdirs();

        File sFile = stateFile(docId);
        if (!sFile.exists()) {
            DocumentState empty = new DocumentState();
            empty.text = "";
            empty.images = new ArrayList<>();
            store.saveObject(empty, sFile);
        }

        File mFile = metaFile(docId);
        if (!mFile.exists()) {
            long now = System.currentTimeMillis();
            DocumentMeta meta = new DocumentMeta(docId, "Untitled", now);
            store.saveObject(meta, mFile);
        }
    }

    public synchronized DocumentState load(String docId) {
        ensureExists(docId);
        return store.loadObject(stateFile(docId), DocumentState.class);
    }

    public synchronized void save(String docId, DocumentState state) {
        ensureExists(docId);

        store.saveObject(state, stateFile(docId));

        DocumentMeta meta = getMeta(docId);
        if (meta == null) meta = new DocumentMeta(docId, "Untitled", System.currentTimeMillis());
        meta.updatedAt = System.currentTimeMillis();
        if (meta.title == null || meta.title.isBlank()) meta.title = "Untitled";
        store.saveObject(meta, metaFile(docId));
    }

    public synchronized DocumentMeta getMeta(String docId) {
        if (docId == null || docId.isBlank()) return null;
        ensureExists(docId);
        return store.loadObject(metaFile(docId), DocumentMeta.class);
    }

    public synchronized String getTitle(String docId) {
        DocumentMeta meta = getMeta(docId);
        if (meta == null) return "Untitled";
        return (meta.title == null || meta.title.isBlank()) ? "Untitled" : meta.title;
    }

    // 문서 목록
    public synchronized List<DocumentMeta> listMetas() {
        if (!rootDir.exists()) rootDir.mkdirs();

        File[] dirs = rootDir.listFiles(File::isDirectory);
        if (dirs == null) return new ArrayList<>();

        List<DocumentMeta> result = new ArrayList<>();
        for (File d : dirs) {
            String docId = d.getName();
            try {
                DocumentMeta meta = store.loadObject(metaFile(docId), DocumentMeta.class);
                if (meta != null) result.add(meta);
            } catch (Exception ignored) {}
        }

        result.sort((a, b) -> Long.compare(b.updatedAt, a.updatedAt)); // 최신순
        return result;
    }

    // 새 문서 생성
    public synchronized DocumentMeta create(String title) {
        String docId = UUID.randomUUID().toString().replace("-", "");
        ensureExists(docId);

        DocumentMeta meta = getMeta(docId);
        if (meta == null) meta = new DocumentMeta(docId, "Untitled", System.currentTimeMillis());
        meta.title = (title == null || title.isBlank()) ? "Untitled" : title;
        meta.updatedAt = System.currentTimeMillis();
        store.saveObject(meta, metaFile(docId));

        DocumentState empty = new DocumentState();
        empty.text = "";
        empty.images = new ArrayList<>();
        store.saveObject(empty, stateFile(docId));

        return meta;
    }

    // 문서 삭제
    public synchronized boolean delete(String docId) {
        if (docId == null || docId.isBlank()) return false;
        File dir = docDir(docId);
        if (!dir.exists()) return false;
        return deleteRecursively(dir);
    }

    private boolean deleteRecursively(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursively(c);
            }
        }
        return f.delete();
    }
}
