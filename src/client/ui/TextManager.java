package client.ui;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.text.Element;
import javax.swing.text.DefaultHighlighter;

public class TextManager {

    private final JTextComponent editor;
    private DocumentChangeListener changeListener;
    private boolean ignoreEvents = false;
    private boolean listenerRegistered = false;
    private Map<Integer, Object> highlightTags = new HashMap<>();
    private Object currentLineHighlightTag = null;


    public interface DocumentChangeListener {
        void onTextInserted(int offset, String text);
        void onTextDeleted(int offset, int length);
        void onFullDocumentChanged(String text);
    }

    public TextManager(JTextComponent editor) {
        this.editor = editor;
    }

    public void setChangeListener(DocumentChangeListener listener) {
        this.changeListener = listener;
    }

    public void registerListener() {
        if (listenerRegistered) return;
        listenerRegistered = true;

        editor.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                if (ignoreEvents) return;

                try {
                    int offset = e.getOffset();
                    int length = e.getLength();

                    // Document에서 직접 읽어서 삽입된 텍스트 확보
                    String inserted = editor.getDocument().getText(offset, length);

                    if (changeListener != null) {
                        changeListener.onTextInserted(offset, inserted);
                    }
                } catch (Exception ignored) {}
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (ignoreEvents) return;

                if (changeListener != null) {
                    changeListener.onTextDeleted(e.getOffset(), e.getLength());
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    // 원격 텍스트 삽입 적용
    public void applyInsert(int offset, String text) {
        ignoreEvents = true;
        try {
            Document doc = editor.getDocument();
            int safeOffset = Math.max(0, Math.min(offset, doc.getLength()));
            try {
                doc.insertString(safeOffset, text, null);
            } catch (BadLocationException e) {
                // offset이 범위를 살짝 벗어난 경우 방어
            }
        } finally {
            ignoreEvents = false;
        }
    }

    // 원격 텍스트 삭제 적용
    public void applyDelete(int offset, int length) {
        ignoreEvents = true;
        try {
            Document doc = editor.getDocument();
            int docLen = doc.getLength();

            if (offset < 0) return;
            if (offset > docLen) return;

            int actualLength = Math.min(length, docLen - offset);
            if (actualLength > 0) {
                try {
                    doc.remove(offset, actualLength);
                } catch (BadLocationException e) {
                }
            }
        } finally {
            ignoreEvents = false;
        }
    }

    public void setFullDocument(String text) {
        ignoreEvents = true;
        try {
            editor.setText(text);
        } finally {
            ignoreEvents = false;
        }
    }

    public String getFullText() {
        return editor.getText();
    }

    public void setIgnoreEvents(boolean ignore) {
        this.ignoreEvents = ignore;
    }

    public void highlightLine(int lineIndex, Color color) {
        try {
            Highlighter h = editor.getHighlighter();
            Document doc = editor.getDocument();

            // 해당 줄의 시작과 끝 offset 계산
            int lineStart = getLineStartOffset(lineIndex);
            int lineEnd = getLineEndOffset(lineIndex);

            if (lineStart < 0 || lineEnd < 0) return;

            // 이미 하이라이트 되어있으면 제거
            if (highlightTags.containsKey(lineIndex)) {
                h.removeHighlight(highlightTags.get(lineIndex));
            }

            // 새로 하이라이트
            Object tag = h.addHighlight(lineStart, lineEnd,
                    new DefaultHighlighter.DefaultHighlightPainter(color));
            highlightTags.put(lineIndex, tag);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearLineHighlight(int lineIndex) {
        try {
            Highlighter h = editor.getHighlighter();
            if (highlightTags.containsKey(lineIndex)) {
                h.removeHighlight(highlightTags.get(lineIndex));
                highlightTags.remove(lineIndex);
            }
        } catch (Exception ignored) {}
    }

    public void clearAllLineHighlights() {
        try {
            Highlighter h = editor.getHighlighter();
            for (Object tag : highlightTags.values()) {
                h.removeHighlight(tag);
            }
            highlightTags.clear();
        } catch (Exception ignored) {}
    }

    private int getLineStartOffset(int lineIndex) {
        try {
            Document doc = editor.getDocument();
            int offset = 0;
            int currentLine = 0;

            for (int i = 0; i < doc.getLength(); i++) {
                if (currentLine == lineIndex) return offset;
                if (doc.getText(i, 1).equals("\n")) {
                    currentLine++;
                    offset = i + 1;
                }
            }
            return (currentLine == lineIndex) ? offset : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private int getLineEndOffset(int lineIndex) {
        try {
            Document doc = editor.getDocument();
            int offset = 0;
            int currentLine = 0;

            for (int i = 0; i < doc.getLength(); i++) {
                if (currentLine == lineIndex && doc.getText(i, 1).equals("\n")) {
                    return i;
                }
                if (doc.getText(i, 1).equals("\n")) {
                    currentLine++;
                    offset = i + 1;
                }
            }
            return (currentLine == lineIndex) ? doc.getLength() : -1;
        } catch (Exception e) {
            return -1;
        }
    }
}

