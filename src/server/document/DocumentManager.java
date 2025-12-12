package server.document;

import global.enums.Mode;
import global.object.DocumentState;
import global.object.EditMessage;
import global.object.ImageState;

import java.util.*;

public class DocumentManager {

    private static final char IMAGE_PLACEHOLDER = '\uFFFC';
    private final StringBuilder text = new StringBuilder();
    private final Map<Integer, ImageBlock> images = new HashMap<>();

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

        text.delete(start, end);
        int delta = end - start;

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
    }

    private void applyImageInsert(EditMessage msg) {
        if (msg.payload == null) return;
        int offset = clampOffset(msg.offset);

        text.insert(offset, IMAGE_PLACEHOLDER);
        shiftImages(offset, 1);

        int w = msg.width > 0 ? msg.width : -1;
        int h = msg.height > 0 ? msg.height : -1;

        ImageBlock block = new ImageBlock(msg.blockId, offset, w, h, msg.payload);
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
        if (oldOffset < text.length() && text.charAt(oldOffset) == IMAGE_PLACEHOLDER) {
            text.delete(oldOffset, oldOffset + 1);
        }
        for (ImageBlock b : images.values()) {
            if (b.id == block.id) continue;
            if (b.offset > oldOffset) b.offset -= 1;
        }

        if (newOffset > oldOffset) newOffset -= 1;
        newOffset = clampOffset(newOffset);

        text.insert(newOffset, IMAGE_PLACEHOLDER);
        for (ImageBlock b : images.values()) {
            if (b.id == block.id) continue;
            if (b.offset >= newOffset) b.offset += 1;
        }
        block.offset = newOffset;
    }

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

    public synchronized List<EditMessage> buildFullImageSyncMessages(String docId, String userId) {
        List<EditMessage> result = new ArrayList<>();
        for (ImageBlock b : images.values()) {
            EditMessage msg = new EditMessage(Mode.IMAGE_INSERT, userId, null);
            msg.docId = docId;
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

    public synchronized DocumentState createState() {
        DocumentState s = new DocumentState();
        s.text = text.toString();
        s.images = new ArrayList<>();
        for (ImageBlock b : images.values()) {
            s.images.add(b.toState());
        }
        return s;
    }

    public synchronized void loadState(DocumentState state) {
        text.setLength(0);
        images.clear();
        if (state == null) return;

        if (state.text != null) text.append(state.text);
        if (state.images != null) {
            for (ImageState is : state.images) {
                images.put(is.id, ImageBlock.fromState(is));
            }
        }
    }
}
