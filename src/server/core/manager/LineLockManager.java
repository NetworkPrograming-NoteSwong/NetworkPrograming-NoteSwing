package server.core.manager;

import java.util.HashMap;
import java.util.Map;

public class LineLockManager {

    // key: 줄 번호, value: userId
    private final Map<Integer, String> lineLocks = new HashMap<>();

    // 잠금 시도: 성공하면 true, 이미 잠겨 있으면 false
    public synchronized boolean tryLock(int lineIndex, String userId) {
        if (lineLocks.containsKey(lineIndex)) {
            return false;
        }
        lineLocks.put(lineIndex, userId);
        return true;
    }

    // 잠금 해제 (소유자만 해제 가능)
    public synchronized void unlock(int lineIndex, String userId) {
        String owner = lineLocks.get(lineIndex);
        if (userId.equals(owner)) {
            lineLocks.remove(lineIndex);
        }
    }

    public synchronized String getOwner(int lineIndex) {
        return lineLocks.get(lineIndex);  // null이면 아직 안 잠김
    }
}
