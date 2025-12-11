package client.ui;

public interface ImageEventListener {

    // 이 클라이언트에서 새 이미지를 삽입했을 때
    void onLocalImageInserted(int blockId, int offset, int width, int height, byte[] data);

    // 이 클라이언트에서 이미지 크기를 바꿨을 때 (우클릭/휠 등으로)
    void onLocalImageResized(int blockId, int width, int height);

    // 이 클라이언트에서 이미지를 다른 위치로 옮겼을 때 (드래그 등으로)
    void onLocalImageMoved(int blockId, int newOffset);
}
