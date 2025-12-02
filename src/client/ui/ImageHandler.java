package client.ui;

import client.controller.EditorController;
import server.core.manager.DocumentManager;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * 책임: 이미지 관련 모든 로직
 * - 이미지 드롭/붙여넣기
 * - 이미지 삽입/리사이즈
 * - PNG 인코딩/디코딩
 * ⭐ 핵심: offset 기반 정확한 이미지 삽입
 */
public class ImageHandler {

    private static final char IMAGE_PLACEHOLDER = DocumentManager.IMAGE_PLACEHOLDER;

    private final JTextPane editor;
    private final JScrollPane scrollPane;
    private final EditorController controller;

    public ImageHandler(JTextPane editor, JScrollPane scrollPane, EditorController controller) {
        this.editor = editor;
        this.scrollPane = scrollPane;
        this.controller = controller;
    }

    /**
     * TransferHandler 설치 (드롭/붙여넣기)
     */
    public void setupImageTransferHandler() {
        editor.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.imageFlavor) ||
                        support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
                        support.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                try {
                    if (!canImport(support)) return false;

                    // 이미지 드롭/붙여넣기
                    if (support.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                        Transferable t = support.getTransferable();
                        Image img = (Image) t.getTransferData(DataFlavor.imageFlavor);
                        insertLocalImage(convertToBufferedImage(img));
                        return true;
                    }

                    // 파일 드롭 (이미지 파일)
                    if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        Transferable t = support.getTransferable();
                        @SuppressWarnings("unchecked")
                        List<java.io.File> files =
                                (List<java.io.File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                        for (java.io.File f : files) {
                            try {
                                BufferedImage bi = ImageIO.read(f);
                                if (bi != null) {
                                    insertLocalImage(bi);
                                }
                            } catch (Exception ignored) {}
                        }
                        return true;
                    }

                    // 일반 텍스트 붙여넣기
                    if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        String s = (String) support.getTransferable()
                                .getTransferData(DataFlavor.stringFlavor);
                        editor.replaceSelection(s);
                        return true;
                    }

                    return false;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        });
    }

    /**
     * 서버에서 이미지 삽입 메시지 받을 때 호출
     * ⭐ offset을 정확히 유지하면서 삽입
     */
    public void applyImageInsert(int blockId, int offset, byte[] data, int width, int height) {
        insertImageIntoDocument(blockId, offset, data, width, height);
    }

    /**
     * 서버에서 이미지 리사이즈 메시지 받을 때 호출
     */
    public void applyImageResize(int blockId, int newWidth, int newHeight) {
        try {
            StyledDocument doc = editor.getStyledDocument();
            int len = doc.getLength();
            for (int i = 0; i < len; i++) {
                Element elem = doc.getCharacterElement(i);
                Object bid = elem.getAttributes().getAttribute("blockId");
                if (bid instanceof Integer && ((Integer) bid) == blockId) {
                    Icon icon = StyleConstants.getIcon(elem.getAttributes());
                    if (icon instanceof ImageIcon imageIcon) {
                        Image original = imageIcon.getImage();
                        Image scaled = original.getScaledInstance(
                                newWidth, newHeight, Image.SCALE_SMOOTH);
                        ImageIcon newIcon = new ImageIcon(scaled);

                        SimpleAttributeSet attrs = new SimpleAttributeSet();
                        StyleConstants.setIcon(attrs, newIcon);
                        attrs.addAttribute("blockId", blockId);

                        doc.setCharacterAttributes(i, 1, attrs, true);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 실제 문서에 이미지 삽입
     * ⭐ 핵심: offset이 정확해야 함
     */
    private void insertImageIntoDocument(int blockId, int offset, byte[] data, int width, int height) {
        try {
            StyledDocument doc = editor.getStyledDocument();
            int docLen = doc.getLength();

            // offset 보정
            if (offset < 0) offset = 0;
            if (offset > docLen) offset = docLen;

            // 바이트 → BufferedImage
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) return;

            // 스케일
            Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            ImageIcon icon = new ImageIcon(scaled);

            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setIcon(attrs, icon);
            attrs.addAttribute("blockId", blockId);

            // 플레이스홀더 있는지 확인
            boolean placeholderExists = false;
            if (offset < doc.getLength()) {
                String ch = doc.getText(offset, 1);
                if (!ch.isEmpty() && ch.charAt(0) == IMAGE_PLACEHOLDER) {
                    placeholderExists = true;
                }
            }

            if (placeholderExists) {
                doc.setCharacterAttributes(offset, 1, attrs, true);
            } else {
                doc.insertString(offset, String.valueOf(IMAGE_PLACEHOLDER), attrs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 로컬에서 이미지 삽입
     */
    private void insertLocalImage(BufferedImage image) {
        if (controller == null || image == null) return;

        int caret = editor.getCaretPosition();
        Dimension size = computeInitialImageSize(image);
        byte[] bytes = encodeImageToPng(image);

        if (bytes == null) return;

        int blockId = controller.onImageInserted(caret, bytes, size.width, size.height);
        insertImageIntoDocument(blockId, caret, bytes, size.width, size.height);
    }

    private BufferedImage convertToBufferedImage(Image img) {
        BufferedImage bi = new BufferedImage(
                img.getWidth(null),
                img.getHeight(null),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2 = bi.createGraphics();
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        return bi;
    }

    private Dimension computeInitialImageSize(BufferedImage image) {
        int originalW = image.getWidth();
        int originalH = image.getHeight();

        int maxWidth = 600;
        if (scrollPane != null && scrollPane.getViewport().getWidth() > 0) {
            maxWidth = (int) (scrollPane.getViewport().getWidth() * 0.7);
        }

        double scale = 1.0;
        if (originalW > maxWidth) {
            scale = (double) maxWidth / (double) originalW;
        }

        int w = (int) Math.max(50, originalW * scale);
        int h = (int) Math.max(50, originalH * scale);
        return new Dimension(w, h);
    }

    private byte[] encodeImageToPng(BufferedImage image) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bos);
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}
