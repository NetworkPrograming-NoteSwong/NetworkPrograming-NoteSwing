package server.core;

import global.enums.Mode;
import global.object.EditMessage;
import global.object.DocumentState;
import global.object.ImageState;

import java.util.*;

public class DocumentManager {

    private static final char IMAGE_PLACEHOLDER = '\uFFFC';
    private final StringBuilder text = new StringBuilder();
    private final Map<Integer, ImageBlock> images = new HashMap<>();

    // 서버 내부에서만 쓰는 이미지 모델
    private static class ImageBlock {
        final int id;
        int offset;
        int width;
        int height;
        final byte[] data;

        ImageBlock(int id, int offset, int width, int height, byte[] data) {
            this.id = id;
            this.offset = offset;
            this.width = width;
            this.height = height;
            this.data = data;
        }

        ImageState toState() {
            ImageState s = new ImageState();
            s.id = id;
            s.offset = offset;
            s.width = width;
            s.height = height;
            s.data = data;
            return s;
        }

        static ImageBlock fromState(ImageState s) {
            return new ImageBlock(s.id, s.offset, s.width, s.height, s.data);
        }
    }

    // === 외부에서 들어온 메시지 적용 ===
    public synchronized void apply(EditMessage msg) {
        if (msg == null || msg.mode == null) return;
        switch (msg.mode) {
            case INSERT -> applyInsert(msg);
            case DELETE -> applyDelete(msg);
            case FULL_SYNC -> applyFullSync(msg);
            case IMAGE_INSERT -> applyImageInsert(msg);
            case IMAGE_RESIZE -> applyImageResize(msg);
            case IMAGE_MOVE -> applyImageMove(msg);
            default -> {}
        }
    }

    private void applyInsert(EditMessage msg) {
        if (msg.text == null || msg.text.isEmpty()) return;
        int offset = clampOffset(msg.offset);
        text.insert(offset, msg.text);
        shiftImages(offset, msg.text.length());
    }

    private void applyDelete(EditMessage msg) {
        int start = clampOffset(msg.offset);
        int end = clampOffset(msg.offset + msg.length);
        if (start >= end) return;

        // 텍스트 삭제
        text.delete(start, end);
        int delta = end - start;

        // 이미지: 삭제 구간 안에 있으면 제거, 그 이후는 왼쪽으로 당김
        Iterator<ImageBlock> it = images.values().iterator();
        while (it.hasNext()) {
            ImageBlock b = it.next();
            if (b.offset >= start && b.offset < end) {
                it.remove();
            } else if (b.offset >= end) {
                b.offset -= delta;
            }
        }
    }

    private void applyFullSync(EditMessage msg) {
        text.setLength(0);
        if (msg.text != null) text.append(msg.text);
        // 이미지 리스트는 그대로 두거나, 필요시 정책 조정 가능
    }

    private void applyImageInsert(EditMessage msg) {
        if (msg.payload == null) return;
        int offset = clampOffset(msg.offset);

        // 텍스트에 placeholder 삽입
        text.insert(offset, IMAGE_PLACEHOLDER);
        shiftImages(offset, 1);

        int width = msg.width > 0 ? msg.width : -1;
        int height = msg.height > 0 ? msg.height : -1;

        ImageBlock block = new ImageBlock(msg.blockId, offset, width, height, msg.payload);
        images.put(block.id, block);
    }

    private void applyImageResize(EditMessage msg) {
        ImageBlock block = images.get(msg.blockId);
        if (block == null) return;
        if (msg.width > 0) block.width = msg.width;
        if (msg.height > 0) block.height = msg.height;
    }

    private void applyImageMove(EditMessage msg) {
        ImageBlock block = images.get(msg.blockId);
        if (block == null) return;

        int oldOffset = clampOffset(block.offset);
        int newOffset = clampOffset(msg.newOffset);
        if (oldOffset == newOffset) return;

        // 1) 기존 위치의 placeholder 제거
        if (oldOffset < text.length() && text.charAt(oldOffset) == IMAGE_PLACEHOLDER) {
            text.delete(oldOffset, oldOffset + 1);
        }
        // 다른 이미지들은 oldOffset 이후 index -1
        for (ImageBlock b : images.values()) {
            if (b.id == block.id) continue;
            if (b.offset > oldOffset) b.offset -= 1;
        }

        // 삭제 이후 index 보정
        if (newOffset > oldOffset) newOffset -= 1;
        newOffset = clampOffset(newOffset);

        // 2) 새 위치에 placeholder 삽입
        text.insert(newOffset, IMAGE_PLACEHOLDER);
        for (ImageBlock b : images.values()) {
            if (b.id == block.id) continue;
            if (b.offset >= newOffset) b.offset += 1;
        }
        block.offset = newOffset;
    }

    // fromOffset 이후의 이미지 offset 을 delta만큼 이동
    private void shiftImages(int fromOffset, int delta) {
        if (delta == 0) return;
        for (ImageBlock b : images.values()) {
            if (b.offset >= fromOffset) b.offset += delta;
        }
    }

    private int clampOffset(int offset) {
        if (offset < 0) return 0;
        if (offset > text.length()) return text.length();
        return offset;
    }

    public synchronized String getDocument() {
        return text.toString();
    }

    // 새 클라이언트에게 전체 이미지 동기화용 메시지 목록
    public synchronized java.util.List<EditMessage> buildFullImageSyncMessages(String userId) {
        java.util.List<EditMessage> result = new java.util.ArrayList<>();
        for (ImageBlock b : images.values()) {
            EditMessage msg = new EditMessage(Mode.IMAGE_INSERT, userId, null);
            msg.blockId = b.id;
            msg.offset = b.offset;
            msg.length = 1;
            msg.payload = b.data;
            msg.width = b.width;
            msg.height = b.height;
            result.add(msg);
        }
        return result;
    }

    // ==== 파일 저장/불러오기용 스냅샷 ====
    public synchronized DocumentState createState() {
        DocumentState state = new DocumentState();
        state.text = text.toString();
        state.images = new java.util.ArrayList<>();
        for (ImageBlock b : images.values()) {
            state.images.add(b.toState());
        }
        return state;
    }

    public synchronized void loadState(DocumentState state) {
        text.setLength(0);
        images.clear();
        if (state == null) return;
        if (state.text != null) text.append(state.text);
        if (state.images != null) {
            for (ImageState s : state.images) {
                images.put(s.id, ImageBlock.fromState(s));
            }
        }
    }
}
