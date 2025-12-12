package client.ui.image;

import client.ui.TextManager;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

public class ImageMover {

    private final JTextPane editor;
    private final TextManager textManager;
    private final ImageStore store;

    public ImageMover(JTextPane editor, TextManager textManager, ImageStore store) {
        this.editor = editor;
        this.textManager = textManager;
        this.store = store;
    }

    public ImageInfo moveLocal(int blockId, int targetOffset) {
        ImageInfo info = store.get(blockId);
        if (info == null) return null;
        moveInternal(info, targetOffset);
        return store.get(blockId);
    }

    public void applyRemoteMove(int blockId, int targetOffset) {
        ImageInfo info = store.get(blockId);
        if (info == null) return;
        moveInternal(info, targetOffset);
    }

    private void moveInternal(ImageInfo info, int targetOffset) {
        textManager.setIgnoreEvents(true);
        try {
            StyledDocument doc = editor.getStyledDocument();
            int docLen = doc.getLength();

            int oldOffset = info.offset;
            int newOffset = Math.max(0, Math.min(targetOffset, docLen));
            if (oldOffset == newOffset) return;

            // 1) 기존 위치 1글자 제거 (아이콘 placeholder)
            if (oldOffset >= 0 && oldOffset < docLen) {
                doc.remove(oldOffset, 1);
                store.shiftForMoveRemove(info.id, oldOffset);
                if (newOffset > oldOffset) newOffset--;
            }

            int safeNew = Math.max(0, Math.min(newOffset, doc.getLength()));
            editor.setCaretPosition(safeNew);

            // 2) 새 위치에 삽입 (아이콘 placeholder 1글자 추가)
            store.shiftForMoveInsert(info.id, safeNew);
            ImageIcon icon = ImageIOHelper.createScaledIcon(info.data, info.width, info.height);
            editor.insertIcon(icon);

            info.offset = safeNew;
            store.move(info.id, safeNew);
        } catch (BadLocationException ignored) {
        } finally {
            textManager.setIgnoreEvents(false);
        }
    }
}
