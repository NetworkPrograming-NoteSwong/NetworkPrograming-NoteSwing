package server.document;

import global.object.ImageState;

public class ImageBlock {

    public final int id;
    public int offset;
    public int width;
    public int height;
    public final byte[] data;

    public ImageBlock(int id, int offset, int width, int height, byte[] data) {
        this.id = id;
        this.offset = offset;
        this.width = width;
        this.height = height;
        this.data = data;
    }

    public ImageState toState() {
        ImageState s = new ImageState();
        s.id = id;
        s.offset = offset;
        s.width = width;
        s.height = height;
        s.data = data;
        return s;
    }

    public static ImageBlock fromState(ImageState s) {
        return new ImageBlock(s.id, s.offset, s.width, s.height, s.data);
    }
}
