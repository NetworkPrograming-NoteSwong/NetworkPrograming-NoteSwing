package client.ui;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

/**
 * 문서(JTextComponent) 관리 및 변경 감지
 */
public class TextManager {

    private final JTextComponent editor;
    private DocumentChangeListener changeListener;
    private boolean ignoreEvents = false;

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
        editor.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                if (ignoreEvents) return;

                try {
                    int offset = e.getOffset();
                    int length = e.getLength();

                    // ✅ 여기! editor.getText() 대신 Document에서 직접 읽기
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
                // 스타일 변경 이벤트 → 여기서는 무시
            }
        });
    }


    // ★ 여기 두 메서드가 문제였던 부분입니다.
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
                    // 방어 코드
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
}
