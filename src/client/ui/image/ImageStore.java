package client.ui.image;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ImageStore {

    private final Map<Integer, ImageInfo> images = new HashMap<>();
    private int nextId = 1;

    public ImageInfo createLocalImage(int offset, int width, int height, byte[] data) {
        ImageInfo info = new ImageInfo();
        info.id = nextId++;
        info.offset = offset;
        info.width = width;
        info.height = height;
        info.data = data;
        images.put(info.id, info);
        return info;
    }

    public ImageInfo registerRemoteImage(int id, int offset, int width, int height, byte[] data) {
        ImageInfo info = new ImageInfo();
        info.id = id;
        info.offset = offset;
        info.width = width;
        info.height = height;
        info.data = data;
        images.put(id, info);
        if (id >= nextId) nextId = id + 1;
        return info;
    }

    public ImageInfo get(int id) {
        return images.get(id);
    }

    public void resize(int id, int width, int height) {
        ImageInfo info = images.get(id);
        if (info == null) return;
        if (width > 0) info.width = width;
        if (height > 0) info.height = height;
    }

    public void move(int id, int newOffset) {
        ImageInfo info = images.get(id);
        if (info == null) return;
        info.offset = newOffset;
    }

    // ==== 텍스트 삽입/삭제에 따른 offset 조정 ====
    public void shiftOnInsert(int fromOffset, int delta) {
        if (delta == 0) return;
        for (ImageInfo info : images.values()) {
            if (info.offset >= fromOffset) {
                info.offset += delta;
            }
        }
    }

    public void shiftOnDelete(int start, int length) {
        if (length <= 0) return;
        int end = start + length;

        Iterator<ImageInfo> it = images.values().iterator();
        while (it.hasNext()) {
            ImageInfo info = it.next();
            if (info.offset >= start && info.offset < end) {
                it.remove();
            } else if (info.offset >= end) {
                info.offset -= length;
            }
        }
    }

    // ==== 오프셋으로 이미지 찾기 (우클릭 hit-test에 사용) ====
    public ImageInfo findByOffsetNear(int offset) {
        ImageInfo best = null;
        int bestDist = Integer.MAX_VALUE;

        for (ImageInfo info : images.values()) {
            int dist = Math.abs(info.offset - offset);
            if (dist < bestDist && dist <= 1) { // offset 또는 offset±1 정도까지 허용
                bestDist = dist;
                best = info;
            }
        }
        return best;
    }
}
