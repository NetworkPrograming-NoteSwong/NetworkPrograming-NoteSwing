package client.ui;

import client.ui.image.ImageIOHelper;
import client.ui.image.ImageInfo;
import client.ui.image.ImageStore;
import client.ui.image.ImageTransferSupport;

import javax.swing.*;
import javax.swing.text.StyledDocument;
import java.awt.*;

public class ImageManager {

    private static final int MAX_W = 400;
    private static final int MAX_H = 300;

    private final JTextPane editor;
    private final TextManager textManager;
    private final ImageStore store = new ImageStore();

    private ImageEventListener eventListener;

    public ImageManager(JTextPane editor, TextManager textManager) {
        this.editor = editor;
        this.textManager = textManager;
        // 붙여넣기 / Drag&Drop 은 헬퍼 클래스로 분리
        new ImageTransferSupport(editor, bytes -> insertImageLocal(bytes));
    }

    public void setEventListener(ImageEventListener listener) {
        this.eventListener = listener;
    }

    // ===== 로컬에서 이미지 삽입 =====
    private void insertImageLocal(byte[] data) throws Exception {
        int offset = editor.getCaretPosition();
        Dimension size = ImageIOHelper.calcScaledSize(data, MAX_W, MAX_H);
        int w = size.width;
        int h = size.height;

        insertIconAt(offset, data, w, h);

        ImageInfo info = store.createLocalImage(offset, w, h, data);

        if (eventListener != null) {
            eventListener.onLocalImageInserted(
                    info.id, info.offset, info.width, info.height, info.data);
        }
    }

    // ===== 서버에서 온 IMAGE_INSERT =====
    public void insertImageRemote(int blockId, int offset,
                                  int width, int height, byte[] data) {
        Dimension size;
        if (width > 0 && height > 0) {
            size = new Dimension(width, height);
        } else {
            size = ImageIOHelper.calcScaledSize(data, MAX_W, MAX_H);
        }
        int w = size.width;
        int h = size.height;

        insertIconAt(offset, data, w, h);
        store.registerRemoteImage(blockId, offset, w, h, data);
    }

    // ===== 서버에서 온 IMAGE_RESIZE / IMAGE_MOVE =====
    public void applyRemoteResize(int blockId, int width, int height) {
        store.resize(blockId, width, height);
        // TODO: 실제 아이콘의 크기를 재조정
    }

    public void applyRemoteMove(int blockId, int newOffset) {
        store.move(blockId, newOffset);
        // TODO: 실제 문서 내에서 아이콘 위치 재배치
    }

    // ===== 내부 아이콘 삽입 공통 =====
    private void insertIconAt(int offset, byte[] data, int w, int h) {
        textManager.setIgnoreEvents(true);
        try {
            StyledDocument doc = editor.getStyledDocument();
            int safeOffset = Math.max(0, Math.min(offset, doc.getLength()));
            editor.setCaretPosition(safeOffset);

            ImageIcon icon = ImageIOHelper.createScaledIcon(data, w, h);
            editor.insertIcon(icon);
        } finally {
            textManager.setIgnoreEvents(false);
        }
    }
}
