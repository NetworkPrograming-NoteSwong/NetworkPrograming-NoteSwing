package client.ui;

import client.ui.image.*;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ImageManager {

    private static final int MAX_W = 400;
    private static final int MAX_H = 300;
    private static final char IMAGE_PLACEHOLDER = '\uFFFC';

    private final JTextPane editor;
    private final TextManager textManager;

    private final ImageStore store = new ImageStore();
    private final ImageResizer resizer;
    private final ImageMover mover;

    private ImageEventListener eventListener;
    private Integer draggingImageId = null;

    public ImageManager(JTextPane editor, TextManager textManager) {
        this.editor = editor;
        this.textManager = textManager;
        this.resizer = new ImageResizer(editor, textManager, store);
        this.mover = new ImageMover(editor, textManager, store);

        new ImageTransferSupport(editor, bytes -> insertImageLocal(bytes));

        installContextMenu();
        installWheelZoom();
        installDragMove();
    }

    public void setEventListener(ImageEventListener listener) {
        this.eventListener = listener;
    }

    public void clearAll() {
        draggingImageId = null;
        store.clear();
    }

    public void onTextInserted(int offset, int length) {
        store.shiftOnInsert(offset, length);
    }

    public void onTextDeleted(int offset, int length) {
        store.shiftOnDelete(offset, length);
    }

    // ===== 로컬 이미지 삽입 =====
    private void insertImageLocal(byte[] data) throws Exception {
        int offset = editor.getCaretPosition();
        Dimension size = ImageIOHelper.calcScaledSize(data, MAX_W, MAX_H);
        int w = size.width;
        int h = size.height;

        // 아이콘 삽입은 placeholder 1글자를 추가하므로 store도 +1 shift
        store.shiftOnInsert(offset, 1);
        insertIconAt(offset, data, w, h);

        ImageInfo info = store.createLocalImage(offset, w, h, data);

        if (eventListener != null) {
            eventListener.onLocalImageInserted(info.id, info.offset, info.width, info.height, info.data);
        }
    }

    // ===== 서버 IMAGE_INSERT =====
    public void insertImageRemote(int blockId, int offset, int width, int height, byte[] data) {
        Dimension size = (width > 0 && height > 0)
                ? new Dimension(width, height)
                : ImageIOHelper.calcScaledSize(data, MAX_W, MAX_H);

        int w = size.width;
        int h = size.height;

        StyledDocument doc = editor.getStyledDocument();
        boolean hasPlaceholder = false;

        try {
            if (offset >= 0 && offset < doc.getLength()) {
                String ch = doc.getText(offset, 1);
                hasPlaceholder = (ch != null && ch.length() == 1 && ch.charAt(0) == IMAGE_PLACEHOLDER);
            }
        } catch (Exception ignored) {}

        if (!hasPlaceholder) {
            // 실시간 IMAGE_INSERT(placeholder가 문서에 없음) → 길이 +1 되므로 store도 shift
            store.shiftOnInsert(offset, 1);
            insertIconAt(offset, data, w, h);
        } else {
            // 스냅샷 FULL_SYNC에 이미 placeholder가 존재 → "교체" (길이 변화 0)
            replacePlaceholderWithIcon(offset, data, w, h);
        }

        store.registerRemoteImage(blockId, offset, w, h, data);
    }

    private void replacePlaceholderWithIcon(int offset, byte[] data, int w, int h) {
        textManager.setIgnoreEvents(true);
        try {
            StyledDocument doc = editor.getStyledDocument();
            int safe = Math.max(0, Math.min(offset, doc.getLength()));

            // 1) placeholder 제거
            if (safe < doc.getLength()) {
                try { doc.remove(safe, 1); } catch (BadLocationException ignored) {}
            }

            // 2) 동일 위치에 icon 삽입(placeholder 1글자 추가) → 결과적으로 길이 변화 0
            editor.setCaretPosition(Math.max(0, Math.min(safe, doc.getLength())));
            ImageIcon icon = ImageIOHelper.createScaledIcon(data, w, h);
            editor.insertIcon(icon);

        } finally {
            textManager.setIgnoreEvents(false);
        }
    }

    public void applyRemoteResize(int blockId, int width, int height) {
        resizer.applyRemoteResize(blockId, width, height);
    }

    public void applyRemoteMove(int blockId, int newOffset) {
        mover.applyRemoteMove(blockId, newOffset);
    }

    private void installContextMenu() {
        editor.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) showPopup(e); }
            @Override public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) showPopup(e); }
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

    private void installWheelZoom() {
        editor.addMouseWheelListener(e -> {
            if (!e.isControlDown()) return;
            double factor = (e.getWheelRotation() < 0) ? 1.1 : 0.9;

            int offset = editor.viewToModel2D(e.getPoint());
            if (offset < 0) return;

            resizeImageAtOffset(offset, factor);
            e.consume();
        });
    }

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

    private void installDragMove() {
        editor.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                int pos = editor.viewToModel2D(e.getPoint());
                if (pos < 0) { draggingImageId = null; return; }

                ImageInfo target = store.findByOffsetNear(pos);
                draggingImageId = (target == null) ? null : target.id;
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
                if (pos >= 0) editor.setCaretPosition(pos);
            }
        });
    }

    private void moveImageLocal(int blockId, int newOffset) {
        ImageInfo info = mover.moveLocal(blockId, newOffset);
        if (info == null || eventListener == null) return;
        eventListener.onLocalImageMoved(info.id, info.offset);
    }
}
