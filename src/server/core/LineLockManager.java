package server.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 서버용 (클라이언트가 나가면 기존에 잡아놨던 락들 삭제하기 위해서)
 */
public class LineLockManager {
    private final Map<String, Map<Integer, String>> locks = new ConcurrentHashMap<>();

    // 특정 유저가 가진 모든 락 해제
    public void releaseAllLocksOfUser(String userId) {
        if (userId == null) return;

        for (Map<Integer, String> docLocks : locks.values()) {
            docLocks.entrySet().removeIf(e -> userId.equals(e.getValue()));
        }
    }
}
