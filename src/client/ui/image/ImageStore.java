package client.ui.image;

import java.util.HashMap;
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
}
