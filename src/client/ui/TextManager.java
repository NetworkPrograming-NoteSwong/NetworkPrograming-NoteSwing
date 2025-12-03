// src/client/ui/DocumentManager.java
package client.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * 문서(JTextArea) 관리 및 변경 감지
 * UI 업데이트와 문서 변경을 분리
 */
public class TextManager {
    private final JTextArea editor;
    private DocumentChangeListener changeListener;
    private boolean ignoreEvents = false;

    // ⭐ 콜백 인터페이스: 문서 변경 시 외부에 알림
    public interface DocumentChangeListener {
        void onTextInserted(int offset, String text);
        void onTextDeleted(int offset, int length);
        void onFullDocumentChanged(String text);
    }

    public TextManager(JTextArea editor) {
        this.editor = editor;
    }

    /**
     * 문서 변경 리스너 콜백 설정
     * (보통 Controller가 넣음)
     */
    public void setChangeListener(DocumentChangeListener listener) {
        this.changeListener = listener;
    }

    /**
     * DocumentListener 등록
     * 이후 사용자 입력 시 콜백으로 알림
     */
    public void registerListener() {
        editor.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                if (ignoreEvents) return;

                try {
                    int offset = e.getOffset();
                    int length = e.getLength();
                    String inserted = editor.getText().substring(offset, offset + length);

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
                if (ignoreEvents) return;

                if (changeListener != null) {
                    changeListener.onFullDocumentChanged(editor.getText());
                }
            }
        });
    }

    /**
     * 서버에서 온 INSERT 메시지를 에디터에 적용
     */
    public void applyInsert(int offset, String text) {
        ignoreEvents = true;
        try {
            editor.insert(text, offset);
        } finally {
            ignoreEvents = false;
        }
    }

    /**
     * 서버에서 온 DELETE 메시지를 에디터에 적용
     */
    public void applyDelete(int offset, int length) {
        ignoreEvents = true;
        try {
            if (offset < 0) return;
            if (offset > editor.getDocument().getLength()) return;

            // ⭐ 범위 검증 (한글 IME 처리 때 offset이 꼬일 수 있음)
            int actualLength = Math.min(length, editor.getDocument().getLength() - offset);
            if (actualLength > 0) {
                editor.replaceRange("", offset, offset + actualLength);
            }
        } finally {
            ignoreEvents = false;
        }
    }

    /**
     * 서버에서 온 FULL_SYNC 메시지를 에디터에 적용
     */
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
