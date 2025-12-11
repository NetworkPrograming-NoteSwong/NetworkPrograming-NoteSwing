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

    // 로컬에서 드래그로 이동할 때 사용
    public ImageInfo moveLocal(int blockId, int targetOffset) {
        ImageInfo info = store.get(blockId);
        if (info == null) return null;
        moveInternal(info, targetOffset);
        return store.get(blockId);
    }

    // IMAGE_MOVE 적용
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

            // 기존 위치의 placeholder 삭제
            if (oldOffset >= 0 && oldOffset < docLen) {
                doc.remove(oldOffset, 1);
                // 앞에서 삭제했으니, 목표 위치가 뒤쪽이면 한 칸 당겨야 함
                if (newOffset > oldOffset) {
                    newOffset--;
                }
            }

            int safeNew = Math.max(0, Math.min(newOffset, doc.getLength()));
            editor.setCaretPosition(safeNew);

            ImageIcon icon = ImageIOHelper.createScaledIcon(
                    info.data, info.width, info.height);
            editor.insertIcon(icon);

            info.offset = safeNew;
            store.move(info.id, safeNew);
        } catch (BadLocationException ignored) {
        } finally {
            textManager.setIgnoreEvents(false);
        }
    }
}
