package client.ui;

import client.ui.image.*;

import javax.swing.*;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ImageManager {

    private static final int MAX_W = 400;
    private static final int MAX_H = 300;

    private final JTextPane editor;
    private final TextManager textManager;
    private final ImageStore store = new ImageStore();
    private final ImageResizer resizer;
    private final ImageMover mover;

    private ImageEventListener eventListener;
    private Integer draggingImageId = null; // 드래그 중인 이미지 ID(없으면 null)

    public ImageManager(JTextPane editor, TextManager textManager) {
        this.editor = editor;
        this.textManager = textManager;
        this.resizer = new ImageResizer(editor, textManager, store);
        this.mover = new ImageMover(editor, textManager, store);

        // 붙여넣기 / Drag&Drop
        new ImageTransferSupport(editor, bytes -> insertImageLocal(bytes));

        // 우클릭 메뉴 + Ctrl+휠 줌
        installContextMenu();
        installWheelZoom();
        installDragMove();
    }

    public void setEventListener(ImageEventListener listener) {
        this.eventListener = listener;
    }

    // ===== 텍스트 변경에 따른 offset 조정 =====
    public void onTextInserted(int offset, int length) {
        store.shiftOnInsert(offset, length);
    }

    public void onTextDeleted(int offset, int length) {
        store.shiftOnDelete(offset, length);
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
        resizer.applyRemoteResize(blockId, width, height);
    }

    public void applyRemoteMove(int blockId, int newOffset) {
        mover.applyRemoteMove(blockId, newOffset);
    }

    // ===== 우클릭 컨텍스트 메뉴 =====
    private void installContextMenu() {
        editor.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }
        });
    }

    private void showPopup(MouseEvent e) {
        int pos = editor.viewToModel2D(e.getPoint());
        if (pos < 0) return;

        ImageInfo target = store.findByOffsetNear(pos);
        if (target == null) return;

        JPopupMenu menu = new JPopupMenu();
        JMenuItem bigger = new JMenuItem("이미지 크게");
        JMenuItem smaller = new JMenuItem("이미지 작게");

        bigger.addActionListener(ev -> resizeImageById(target.id, 1.25));
        smaller.addActionListener(ev -> resizeImageById(target.id, 0.8));

        menu.add(bigger);
        menu.add(smaller);
        menu.show(editor, e.getX(), e.getY());
    }

    // ===== Ctrl + 휠 줌 =====
    private void installWheelZoom() {
        editor.addMouseWheelListener(e -> {
            if (!e.isControlDown()) return;

            int rotation = e.getWheelRotation();
            double factor = (rotation < 0) ? 1.1 : 0.9;

            int offset = editor.viewToModel2D(e.getPoint());
            if (offset < 0) return;

            resizeImageAtOffset(offset, factor);
            e.consume();
        });
    }

    // ===== 리사이즈 요청 → Resizer 위임 + 서버 통보 =====
    private void resizeImageById(int blockId, double scale) {
        ImageInfo info = resizer.resizeLocalByBlockId(blockId, scale);
        if (info == null || eventListener == null) return;
        eventListener.onLocalImageResized(info.id, info.width, info.height);
    }

    public void resizeImageAtOffset(int offset, double scale) {
        ImageInfo info = resizer.resizeLocalAtOffset(offset, scale);
        if (info == null || eventListener == null) return;
        eventListener.onLocalImageResized(info.id, info.width, info.height);
    }

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

    // ===== 이미지 드래그 이동 =====
    private void installDragMove() {
        // 마우스 버튼 press/release로 드래그 시작/종료 감지
        editor.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;

                int pos = editor.viewToModel2D(e.getPoint());
                if (pos < 0) {
                    draggingImageId = null;
                    return;
                }

                ImageInfo target = store.findByOffsetNear(pos);
                if (target != null) {
                    draggingImageId = target.id;
                } else {
                    draggingImageId = null;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                if (draggingImageId == null) return;

                int dropPos = editor.viewToModel2D(e.getPoint());
                if (dropPos >= 0) {
                    moveImageLocal(draggingImageId, dropPos);
                }
                draggingImageId = null;
            }
        });

        editor.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggingImageId == null) return;
                int pos = editor.viewToModel2D(e.getPoint());
                if (pos >= 0) {
                    editor.setCaretPosition(pos);
                }
            }
        });
    }

    // ===== 로컬 이동 요청 → mover + 서버 통보 =====
    private void moveImageLocal(int blockId, int newOffset) {
        ImageInfo info = mover.moveLocal(blockId, newOffset);
        if (info == null || eventListener == null) return;
        eventListener.onLocalImageMoved(info.id, info.offset);
    }

}
