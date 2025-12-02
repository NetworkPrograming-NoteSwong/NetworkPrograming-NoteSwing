package client.ui;

import client.controller.EditorController;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 책임: 줄 잠금 관련 모든 로직
 * - 다른 사용자 잠금 추적
 * - DocumentFilter로 입력 차단
 * - 하이라이트 표시
 * - 자동 LOCK/UNLOCK
 */
public class LineLockDecorator {

    private final JTextPane editor;
    private final JLabel modeLabel;
    private final EditorController controller;

    private final Set<Integer> lockedLinesByOthers = new HashSet<>();
    private final Map<Integer, Object> lockHighlights = new HashMap<>();
    private int myLockedLine = -1;

    public LineLockDecorator(JTextPane editor, JLabel modeLabel, EditorController controller) {
        this.editor = editor;
        this.modeLabel = modeLabel;
        this.controller = controller;
    }

    /**
     * DocumentFilter 설치 (잠긴 줄 입력 차단)
     */
    public void installDocumentFilter() {
        AbstractDocument doc = (AbstractDocument) editor.getDocument();
        doc.setDocumentFilter(new DocumentFilter() {

            @Override
            public void insertString(FilterBypass fb, int offset, String str, AttributeSet attrs)
                    throws BadLocationException {

                if (isLockedOffset(offset)) {
                    JOptionPane.showMessageDialog(
                            (JFrame) SwingUtilities.getWindowAncestor(editor),
                            "🔒 이 줄은 다른 사용자가 편집 중입니다.",
                            "편집 불가",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    return;
                }
                super.insertString(fb, offset, str, attrs);
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length)
                    throws BadLocationException {

                if (isLockedOffset(offset)) {
                    JOptionPane.showMessageDialog(
                            (JFrame) SwingUtilities.getWindowAncestor(editor),
                            "🔒 이 줄은 다른 사용자가 편집 중입니다.",
                            "편집 불가",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    return;
                }
                super.remove(fb, offset, length);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length,
                                String text, AttributeSet attrs)
                    throws BadLocationException {

                if (isLockedOffset(offset)) {
                    JOptionPane.showMessageDialog(
                            (JFrame) SwingUtilities.getWindowAncestor(editor),
                            "🔒 이 줄은 다른 사용자가 편집 중입니다.",
                            "편집 불가",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    return;
                }
                super.replace(fb, offset, length, text, attrs);
            }
        });
    }

    /**
     * 커서가 움직였을 때 호출 (자동 잠금/해제)
     * offset이 속한 줄이 바뀌면 자동으로 이전 줄 UNLOCK, 새 줄 LOCK
     */
    public void onCaretMoved(int offset) {
        try {
            int currentLine = getLineOfOffset(offset);

            if (currentLine == myLockedLine) return;

            if (myLockedLine != -1) {
                controller.requestUnlockLine(myLockedLine);
            }

            controller.requestLockLine(currentLine);
            myLockedLine = currentLine;

        } catch (BadLocationException ex) {
            // 무시
        }
    }

    /**
     * 다른 사용자가 줄을 잠갔을 때 호출
     */
    public void lockLine(int lineIndex, String ownerUserId) {
        lockedLinesByOthers.add(lineIndex);

        try {
            int startOffset = getLineStartOffset(lineIndex);
            int endOffset = getLineEndOffset(lineIndex);

            Highlighter highlighter = editor.getHighlighter();

            Object oldTag = lockHighlights.get(lineIndex);
            if (oldTag != null) {
                highlighter.removeHighlight(oldTag);
            }

            Object tag = highlighter.addHighlight(
                    startOffset, endOffset,
                    new DefaultHighlighter.DefaultHighlightPainter(
                            new Color(255, 220, 220)
                    )
            );
            lockHighlights.put(lineIndex, tag);

            modeLabel.setText("모드: TEXT  🔒 line " + (lineIndex + 1) + " (" + ownerUserId + ")");
        } catch (BadLocationException ignored) {}
    }

    /**
     * 줄 잠금이 해제되었을 때 호출
     */
    public void unlockLine(int lineIndex) {
        lockedLinesByOthers.remove(lineIndex);

        Object tag = lockHighlights.remove(lineIndex);
        if (tag != null) {
            editor.getHighlighter().removeHighlight(tag);
        }

        modeLabel.setText("모드: TEXT");
    }

    /**
     * offset이 잠긴 줄에 속하는지 확인
     * ⭐ 핵심: offset이 정확히 어느 줄인지 판정하는 메서드
     */
    private boolean isLockedOffset(int offset) {
        try {
            int line = getLineOfOffset(offset);
            return lockedLinesByOthers.contains(line);
        } catch (BadLocationException e) {
            return false;
        }
    }

    // ===== offset ↔ 라인 계산 유틸 (원본 그대로) =====

    private int getLineOfOffset(int offset) throws BadLocationException {
        Element root = editor.getDocument().getDefaultRootElement();
        return root.getElementIndex(offset);
    }

    private int getLineStartOffset(int line) throws BadLocationException {
        Element root = editor.getDocument().getDefaultRootElement();
        Element lineElem = root.getElement(line);
        if (lineElem == null) {
            throw new BadLocationException("No such line", editor.getDocument().getLength());
        }
        return lineElem.getStartOffset();
    }

    private int getLineEndOffset(int line) throws BadLocationException {
        Element root = editor.getDocument().getDefaultRootElement();
        Element lineElem = root.getElement(line);
        if (lineElem == null) {
            throw new BadLocationException("No such line", editor.getDocument().getLength());
        }
        return lineElem.getEndOffset();
    }
}
