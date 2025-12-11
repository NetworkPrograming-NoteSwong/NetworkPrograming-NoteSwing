package client.ui.image;

import client.ui.TextManager;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.awt.*;

public class ImageResizer {

    private static final int MIN_SIZE = 32;
    private static final int MAX_SIZE = 1200;

    private final JTextPane editor;
    private final TextManager textManager;
    private final ImageStore store;

    public ImageResizer(JTextPane editor, TextManager textManager, ImageStore store) {
        this.editor = editor;
        this.textManager = textManager;
        this.store = store;
    }

    public ImageInfo resizeLocalByBlockId(int blockId, double scale) {
        ImageInfo info = store.get(blockId);
        if (info == null) return null;
        return resizeLocal(info, scale);
    }

    public ImageInfo resizeLocalAtOffset(int offset, double scale) {
        ImageInfo info = store.findByOffsetNear(offset);
        if (info == null) return null;
        return resizeLocal(info, scale);
    }

    public void applyRemoteResize(int blockId, int width, int height) {
        ImageInfo info = store.get(blockId);
        if (info == null) return;

        if (width <= 0) width = info.width;
        if (height <= 0) height = info.height;

        redrawImageIcon(info, width, height);
        store.resize(blockId, width, height);
    }

    private ImageInfo resizeLocal(ImageInfo info, double scale) {
        int newW = clamp((int) Math.round(info.width * scale), MIN_SIZE, MAX_SIZE);
        int newH = clamp((int) Math.round(info.height * scale), MIN_SIZE, MAX_SIZE);

        if (newW == info.width && newH == info.height) {
            return info;
        }

        redrawImageIcon(info, newW, newH);
        store.resize(info.id, newW, newH);
        return store.get(info.id);
    }

    private void redrawImageIcon(ImageInfo info, int w, int h) {
        textManager.setIgnoreEvents(true);
        try {
            StyledDocument doc = editor.getStyledDocument();
            if (info.offset >= 0 && info.offset < doc.getLength()) {
                doc.remove(info.offset, 1);
            }
            insertIconAt(info.offset, info.data, w, h);
        } catch (BadLocationException ignored) {
        } finally {
            textManager.setIgnoreEvents(false);
        }
    }

    private void insertIconAt(int offset, byte[] data, int w, int h) {
        StyledDocument doc = editor.getStyledDocument();
        int safeOffset = Math.max(0, Math.min(offset, doc.getLength()));
        editor.setCaretPosition(safeOffset);

        ImageIcon icon = ImageIOHelper.createScaledIcon(data, w, h);
        editor.insertIcon(icon);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
