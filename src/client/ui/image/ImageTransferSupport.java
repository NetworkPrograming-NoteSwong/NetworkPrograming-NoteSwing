package client.ui.image;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;

public class ImageTransferSupport {

    public interface InsertHandler {
        void handleInsert(byte[] data) throws Exception;
    }

    private final JTextPane editor;
    private final InsertHandler handler;

    public ImageTransferSupport(JTextPane editor, InsertHandler handler) {
        this.editor = editor;
        this.handler = handler;
        installPasteAction();
        installTransferHandler();
    }

    private void installPasteAction() {
        InputMap im = editor.getInputMap();
        ActionMap am = editor.getActionMap();

        Action defaultPaste = am.get(DefaultEditorKit.pasteAction);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK),
                "customPaste");

        am.put("customPaste", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                try {
                    if (cb.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
                        Image img = (Image) cb.getData(DataFlavor.imageFlavor);
                        byte[] bytes = ImageIOHelper.imageToPngBytes(img);
                        handler.handleInsert(bytes);
                    } else {
                        if (defaultPaste != null) defaultPaste.actionPerformed(e);
                    }
                } catch (Exception ex) {
                    if (defaultPaste != null) defaultPaste.actionPerformed(e);
                }
            }
        });
    }

    private void installTransferHandler() {
        TransferHandler defaultHandler = editor.getTransferHandler();

        editor.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                if (support.isDataFlavorSupported(DataFlavor.imageFlavor) ||
                        support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return true;
                }
                return defaultHandler != null && defaultHandler.canImport(support);
            }

            @Override
            public boolean importData(TransferSupport support) {
                try {
                    if (support.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                        Image img = (Image) support.getTransferable()
                                .getTransferData(DataFlavor.imageFlavor);
                        byte[] bytes = ImageIOHelper.imageToPngBytes(img);
                        handler.handleInsert(bytes);
                        return true;
                    } else if (support.isDataFlavorSupported(
                            DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> files =
                                (List<File>) support.getTransferable()
                                        .getTransferData(DataFlavor.javaFileListFlavor);
                        for (File f : files) {
                            if (ImageIOHelper.isImageFile(f)) {
                                Image img = javax.imageio.ImageIO.read(f);
                                byte[] bytes = ImageIOHelper.imageToPngBytes(img);
                                handler.handleInsert(bytes);
                                return true;
                            }
                        }
                    }
                } catch (Exception ignored) {}

                return defaultHandler != null &&
                        defaultHandler.importData(support);
            }
        });
    }
}
