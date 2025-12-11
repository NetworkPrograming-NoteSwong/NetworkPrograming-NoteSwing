package server.core;

import global.enums.Mode;
import global.object.EditMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DocumentManager {

    // JTextPane이 Icon을 표시할 때 실제로 쓰는 특수 문자와 동일한 코드 사용
    private static final char IMAGE_PLACEHOLDER = '\uFFFC';

    private final StringBuilder document = new StringBuilder();

    private static class ImageBlock {
        final int blockId;
        final byte[] data;

        ImageBlock(int blockId, byte[] data) {
            this.blockId = blockId;
            this.data = data;
        }
    }

    private final List<ImageBlock> images = new ArrayList<>();

    public synchronized void apply(EditMessage msg) {
        switch (msg.mode) {
            case INSERT -> applyInsert(msg);
            case DELETE -> applyDelete(msg);
            case FULL_SYNC -> applyFullSync(msg);
            case IMAGE_INSERT -> applyImageInsert(msg);
            default -> {
                // JOIN/LEAVE 등은 여기서 처리 안 함
            }
        }
    }

    private void applyInsert(EditMessage msg) {
        if (msg.text == null || msg.text.isEmpty()) return;

        int offset = clampOffset(msg.offset);
        document.insert(offset, msg.text);
    }

    private void applyDelete(EditMessage msg) {
        int start = clampOffset(msg.offset);
        int end = clampOffset(msg.offset + msg.length);
        if (start >= end) return;

        // 삭제 구간 안에 포함된 이미지 placeholder 인덱스들 찾기
        List<Integer> imageIndicesToRemove = new ArrayList<>();

        for (int i = start; i < end && i < document.length(); i++) {
            if (document.charAt(i) == IMAGE_PLACEHOLDER) {
                int placeholderIdx = getPlaceholderIndex(i);
                if (placeholderIdx >= 0) {
                    imageIndicesToRemove.add(placeholderIdx);
                }
            }
        }

        // 뒤에서부터 제거해야 index 꼬이지 않음
        Collections.sort(imageIndicesToRemove, Collections.reverseOrder());
        for (int idx : imageIndicesToRemove) {
            if (idx >= 0 && idx < images.size()) {
                images.remove(idx);
            }
        }

        document.delete(start, end);
    }

    private void applyFullSync(EditMessage msg) {
        document.setLength(0);
        if (msg.text != null) {
            document.append(msg.text);
        }
        // 여기서는 일단 이미지 목록은 유지 / 혹은 필요시 images.clear()로 초기화 가능
    }

    private void applyImageInsert(EditMessage msg) {
        int offset = clampOffset(msg.offset);

        // 텍스트에도 placeholder 문자 1개 삽입
        document.insert(offset, IMAGE_PLACEHOLDER);

        // 이미지 데이터 저장
        if (msg.payload != null) {
            images.add(new ImageBlock(msg.blockId, msg.payload));
        }
    }

    private int clampOffset(int offset) {
        if (offset < 0) return 0;
        if (offset > document.length()) return document.length();
        return offset;
    }

    // 문서 전체에서 특정 위치까지 등장한 placeholder의 "몇 번째 이미지"인지 계산
    private int getPlaceholderIndex(int pos) {
        int count = 0;
        for (int i = 0; i < document.length() && i <= pos; i++) {
            if (document.charAt(i) == IMAGE_PLACEHOLDER) {
                if (i == pos) {
                    return count;
                }
                count++;
            }
        }
        return -1;
    }

    public synchronized String getDocument() {
        return document.toString();
    }

    public synchronized List<EditMessage> buildFullImageSyncMessages(String userId) {
        List<Integer> placeholderOffsets = new ArrayList<>();
        for (int i = 0; i < document.length(); i++) {
            if (document.charAt(i) == IMAGE_PLACEHOLDER) {
                placeholderOffsets.add(i);
            }
        }

        int count = Math.min(placeholderOffsets.size(), images.size());
        List<EditMessage> result = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            ImageBlock block = images.get(i);

            EditMessage msg = new EditMessage(Mode.IMAGE_INSERT, userId, null);
            msg.offset = placeholderOffsets.get(i);
            msg.length = 1;
            msg.blockId = block.blockId;
            msg.payload = block.data;

            result.add(msg);
        }

        return result;
    }
}
