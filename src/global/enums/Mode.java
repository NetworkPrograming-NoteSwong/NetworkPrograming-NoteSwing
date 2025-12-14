package global.enums;

public enum Mode {
    INSERT,       // 텍스트 삽입
    DELETE,       // 텍스트 삭제
    FULL_SYNC,    // 전체 문서 동기화
    JOIN,         // 클라이언트 접속
    LEAVE,        // 클라이언트 퇴장
    IMAGE_INSERT, // 이미지 삽입
    IMAGE_RESIZE, // 이미지 크기 변경
    IMAGE_MOVE,   // 이미지 위치 이동
    DOC_OPEN,     // 문서 열기 요청
    DOC_LIST,     // 문서 목록 요청/응답
    DOC_CREATE,   // 문서 생성
    DOC_DELETE,   // 문서 삭제
    SYNC_END,     // 스냅샷(이미지 포함) 전송 완료 알림
    LOCK,
    UNLOCK
}
