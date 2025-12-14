package client.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// server.core.LineLockManager
public class LineLockManager {

    // docId -> (lineIndex -> ownerUserId)
    private final Map<String, Map<Integer, String>> locks = new ConcurrentHashMap<>();

    public synchronized String tryLock(String docId, int lineIndex, String userId) {
        Map<Integer, String> docLocks = locks.computeIfAbsent(docId, k -> new HashMap<>());
        String current = docLocks.get(lineIndex);

        if (current == null || current.equals(userId)) {
            // free or already mine → ok
            docLocks.put(lineIndex, userId);
            return userId;
        }
        // 이미 다른 사용자가 보유
        return current;
    }

    public synchronized void unlock(String docId, int lineIndex, String userId) {
        Map<Integer, String> docLocks = locks.get(docId);
        if (docLocks == null) return;

        String current = docLocks.get(lineIndex);
        if (userId.equals(current)) {
            docLocks.remove(lineIndex);
        }
    }

    public synchronized void releaseAllByUser(String userId) {
        for (Map<Integer, String> docLocks : locks.values()) {
            docLocks.entrySet().removeIf(e -> userId.equals(e.getValue()));
        }
    }
}
