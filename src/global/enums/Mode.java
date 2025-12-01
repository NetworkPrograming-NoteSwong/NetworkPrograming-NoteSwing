// src/global/enums/Mode.java
package global.enums;

public enum Mode {
    INSERT,        // 텍스트 삽입
    DELETE,        // 텍스트 삭제
    FULL_SYNC,     // 전체 문서 동기화
    JOIN,          // 클라이언트 접속
    LEAVE,         // 클라이언트 퇴장
    CURSOR,        // 커서 위치
    IMAGE_INSERT,  // 이미지 삽입
    IMAGE_RESIZE   // 이미지 크기 변경 (향후 확장용)
}