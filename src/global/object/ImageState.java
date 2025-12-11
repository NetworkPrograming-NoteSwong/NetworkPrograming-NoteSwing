package global.object;

import java.io.Serializable;

public class ImageState implements Serializable {
    public int id;
    public int offset;
    public int width;
    public int height;
    public byte[] data;
}
