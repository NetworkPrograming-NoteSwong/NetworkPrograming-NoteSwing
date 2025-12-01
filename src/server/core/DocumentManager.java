// src/server/core/DocumentManager.java
package server.core;

import global.enums.Mode;
import global.object.EditMessage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DocumentManager {

    // 텍스트(이미지 자리용 플레이스홀더 포함)를 저장하는 문서
    private final StringBuilder document = new StringBuilder();

    // 서버에서 관리하는 이미지 상태
    public static class ImageState {
        public final int blockId;
        public int offset;     // document 내에서의 위치 (플레이스홀더 문자 위치)
        public int width;
        public int height;
        public byte[] data;    // 이미지 원본 바이트

        public ImageState(int blockId, int offset, int width, int height, byte[] data) {
            this.blockId = blockId;
            this.offset = offset;
            this.width = width;
            this.height = height;
            this.data = data;
        }
    }

    private final List<ImageState> images = new ArrayList<>();

    // 이미지 자리 표시용 문자 (클라이언트 JTextPane에서도 동일 문자 사용)
    public static final char IMAGE_PLACEHOLDER = '\uFFFC';

    public synchronized void apply(EditMessage msg) {

        switch (msg.mode) {
            case INSERT -> {
                if (msg.text == null) return;
                document.insert(msg.offset, msg.text);
                // 텍스트가 들어가면, 그 뒤에 있는 이미지들의 offset을 전부 밀어줘야 함
                shiftImagesOnInsert(msg.offset, msg.text.length());
            }

            case DELETE -> {
                document.delete(msg.offset, msg.offset + msg.length);
                // 삭제된 범위에 있던 이미지는 제거, 그 뒤는 앞으로 당김
                shiftImagesOnDelete(msg.offset, msg.length);
            }

            case FULL_SYNC -> {
                document.setLength(0);
                if (msg.text != null) {
                    document.append(msg.text);
                }
                // FULL_SYNC가 오면 서버 기준으로 완전 새 문서이므로 이미지도 초기화
                images.clear();
            }

            case IMAGE_INSERT -> {
                // 텍스트 안에는 IMAGE_PLACEHOLDER 한 글자를 삽입해서
                // 클라이언트와 서버의 문자열 길이를 맞춰 둔다.
                document.insert(msg.offset, IMAGE_PLACEHOLDER);
                shiftImagesOnInsert(msg.offset, 1);

                // 이미지 상태 추가
                ImageState state = new ImageState(
                        msg.blockId,
                        msg.offset,
                        msg.width,
                        msg.height,
                        msg.payload
                );
                images.add(state);
            }

            case IMAGE_RESIZE -> {
                // 크기 정보만 업데이트 (텍스트에는 영향 없음)
                for (ImageState img : images) {
                    if (img.blockId == msg.blockId) {
                        img.width = msg.width;
                        img.height = msg.height;
                        break;
                    }
                }
            }

            default -> {
                // CURSOR / JOIN / LEAVE 등은 문서 상태를 바꾸지 않음
            }
        }
    }

    public synchronized String getDocument() {
        return document.toString();
    }

    public synchronized List<ImageState> getImages() {
        // 안전하게 복사본 반환
        return new ArrayList<>(images);
    }

    // === 텍스트 삽입/삭제 시 이미지 offset 보정 ===

    private void shiftImagesOnInsert(int offset, int length) {
        for (ImageState img : images) {
            if (img.offset >= offset) {
                img.offset += length;
            }
        }
    }

    private void shiftImagesOnDelete(int offset, int length) {
        int end = offset + length;
        Iterator<ImageState> it = images.iterator();
        while (it.hasNext()) {
            ImageState img = it.next();
            if (img.offset >= offset && img.offset < end) {
                // 삭제 범위 안에 걸친 이미지는 아예 제거
                it.remove();
            } else if (img.offset >= end) {
                // 삭제 범위 뒤에 있는 이미지는 앞으로 당김
                img.offset -= length;
            }
        }
    }
}