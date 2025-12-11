package global.object;

import java.io.Serializable;
import java.util.List;

public class DocumentState implements Serializable {
    public String text;
    public List<ImageState> images;
}
