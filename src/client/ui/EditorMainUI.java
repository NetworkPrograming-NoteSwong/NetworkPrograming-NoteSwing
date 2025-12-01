// src/client/ui/EditorMainUI.java
package client.ui;

import client.controller.EditorController;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EditorMainUI extends JFrame {

    //컨트롤러
    private EditorController controller;

    // 상단 바 컴포넌트
    private JLabel l_loginStatus;
    private JButton b_login;
    private JButton b_logout;

    // 왼쪽 문서 리스트
    private JList<String> list_docs;

    // 중앙 코드 에디터
    private JTextArea t_editor;

    // 하단 상태바
    private JLabel l_connectionStatus;
    private JLabel l_mode;

    // Document 이벤트 플래그 변수
    private boolean ignoreDocumentEvents = false;

    //커서 하이라이트로 표시
    private Map<String, Object> cursorHighlights = new HashMap<>();

    // 다른 사용자가 잠근 줄들
    private final Set<Integer> lockedLinesByOthers = new HashSet<>();

    // 잠긴 줄 하이라이트 태그 (lineIndex -> tag)
    private final Map<Integer, Object> lockHighlights = new HashMap<>();

    public EditorMainUI() {
        super("NoteSwing Client");

        buildGUI();
        installDocumentFilter();

        lockLine(0, "otherUser");

        setSize(1000, 700);
        setLocationRelativeTo(null);               // 화면 중앙
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    // 전체 레이아웃 구성
    private void buildGUI() {
        setLayout(new BorderLayout());

        add(createTopBarPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createStatusBarPanel(), BorderLayout.SOUTH);
    }

    // 상단 TopBar: 앱 이름, 문서 제목, 로그인 상태/버튼
    private JPanel createTopBarPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        p.setBackground(new Color(245, 245, 245));

        // 왼쪽: 앱 이름 + 현재 문서 제목
        JPanel p_left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JLabel l_appName = new JLabel("NoteSwing");
        l_appName.setFont(l_appName.getFont().deriveFont(Font.BOLD, 18f));
        JLabel l_docTitle = new JLabel(" / Untitled Document");

        p_left.add(l_appName);
        p_left.add(l_docTitle);

        // 오른쪽: 로그인 상태 + 버튼들
        JPanel p_right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        l_loginStatus = new JLabel("로그인되지 않음");
        b_login = new JButton("로그인");
        b_logout = new JButton("로그아웃");
        b_logout.setEnabled(false);        // 초기에는 비활성화

        // TODO: 나중에 컨트롤러 연결해서 이벤트 처리
        // b_login.addActionListener(new ActionListener() {
        //     public void actionPerformed(ActionEvent e) {
        //         controller.onClickLogin();
        //     }
        // });
        // b_logout.addActionListener(new ActionListener() {
        //     public void actionPerformed(ActionEvent e) {
        //         controller.onClickLogout();
        //     }
        // });

        p_right.add(l_loginStatus);
        p_right.add(b_login);
        p_right.add(b_logout);

        p.add(p_left, BorderLayout.WEST);
        p.add(p_right, BorderLayout.EAST);

        return p;
    }

    // 중앙: 왼쪽 문서 리스트 + 오른쪽 코드 에디터
    private JComponent createCenterPanel() {
        // 왼쪽 사이드바(문서 리스트)
        JPanel p_sidebar = new JPanel(new BorderLayout());
        p_sidebar.setBorder(BorderFactory.createMatteBorder(
                0, 0, 0, 1, new Color(220, 220, 220)));

        JLabel l_sideTitle = new JLabel("문서");
        l_sideTitle.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p_sidebar.add(l_sideTitle, BorderLayout.NORTH);

        DefaultListModel<String> model = new DefaultListModel<String>();
        model.addElement("Untitled Document");
        model.addElement("Project Plan");
        model.addElement("README.md");

        list_docs = new JList<String>(model);
        p_sidebar.add(new JScrollPane(list_docs), BorderLayout.CENTER);

        // TODO: 문서 선택 이벤트도 나중에 컨트롤러에 연결
        // list_docs.addListSelectionListener(new ListSelectionListener() { ... });

        JPanel p_editor = new JPanel(new BorderLayout());
        t_editor = new JTextArea();
        t_editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        p_editor.add(new JScrollPane(t_editor), BorderLayout.CENTER);

        // 좌우 분할
        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                p_sidebar,
                p_editor
        );
        split.setDividerLocation(220);
        split.setOneTouchExpandable(true);

        return split;
    }

    // 하단 StatusBar: 서버 연결 상태, 모드 표시
    private JPanel createStatusBarPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        p.setBackground(new Color(250, 250, 250));

        l_connectionStatus = new JLabel("서버 연결: 끊김");
        l_mode = new JLabel("모드: TEXT");

        p.add(l_connectionStatus, BorderLayout.WEST);
        p.add(l_mode, BorderLayout.EAST);

        return p;
    }

    // ===== 나중에 컨트롤러/모델에서 호출할 메서드들 =====
    public void updateLoginStatus(String text) {
        l_loginStatus.setText(text);
    }

    public void updateConnectionStatus(String text) {
        l_connectionStatus.setText(text);
    }

    // 내가 직접 타이핑/삭제한 변경을 감지해서 컨트롤러에 알려주는 역할(컨트롤러가 객체로 만들어 서버로 전송)
    private void registerDocumentListener() {
        t_editor.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                if (ignoreDocumentEvents) return;

                try {
                    int offset = e.getOffset();
                    int length = e.getLength();
                    int line = t_editor.getLineOfOffset(offset);

                    String inserted = t_editor.getText().substring(offset, offset + length);
                    controller.onTextInserted(offset, inserted);
                } catch (Exception ignored) {}
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (ignoreDocumentEvents) return;
                controller.onTextDeleted(e.getOffset(), e.getLength());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (ignoreDocumentEvents) return;
                // 스타일/속성 변화 등으로 문서가 바뀌었다고 판단되는 경우(A문서에서 B문서로 이동할 때)
                // 전체 문서를 한 번에 서버로 보내 FULL_SYNC 하도록 함
                String fullText = t_editor.getText();
                controller.onFullDocumentChanged(fullText);

            }
        });
    }

    private void registerCaretListener() {
        t_editor.addCaretListener(e -> {
            if (ignoreDocumentEvents) return;

            int dot = e.getDot();   // 현재 커서 위치
            int mark = e.getMark(); // 선택 시작 위치 (선택 없으면 dot와 같음)

            int start = Math.min(dot, mark);
            int length = Math.abs(dot - mark); // 0이면 단일 커서

            // 컨트롤러에게 “커서/선택 변경됨” 알림
            controller.onCursorMoved(start, length);
        });
    }

    // ===== DocumentFilter: 잠긴 줄(line lock)은 아예 입력/삭제를 막는다 =====
    private void installDocumentFilter() {
        AbstractDocument doc = (AbstractDocument) t_editor.getDocument();
        doc.setDocumentFilter(new DocumentFilter() {

            @Override
            public void insertString(FilterBypass fb, int offset, String str, AttributeSet attrs)
                    throws BadLocationException {

                if (!ignoreDocumentEvents && isLockedOffset(offset)) {
                    JOptionPane.showMessageDialog(
                            EditorMainUI.this,
                            "🔒 이 줄은 다른 사용자가 편집 중입니다.",
                            "편집 불가",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    return; // 문서 변경 자체를 막음
                }

                super.insertString(fb, offset, str, attrs);
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length)
                    throws BadLocationException {

                if (!ignoreDocumentEvents && isLockedOffset(offset)) {
                    JOptionPane.showMessageDialog(
                            EditorMainUI.this,
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

                if (!ignoreDocumentEvents && isLockedOffset(offset)) {
                    JOptionPane.showMessageDialog(
                            EditorMainUI.this,
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

    // offset이 잠긴 줄에 속하는지 확인
    private boolean isLockedOffset(int offset) {
        try {
            int line = t_editor.getLineOfOffset(offset);
            return lockedLinesByOthers.contains(line);
        } catch (BadLocationException e) {
            return false;
        }
    }


    // 다른 사용자가 편집한 결과를 우리 에디터에 반영할 때만 쓰는 메서드(밑에 3개)
    public void applyInsert(int offset, String text) {
        ignoreDocumentEvents = true;
        t_editor.insert(text, offset);
        ignoreDocumentEvents = false;
    }

    public void applyDelete(int offset, int length) {
        ignoreDocumentEvents = true;
        try {
            t_editor.replaceRange("", offset, offset + length);
        } finally {
            ignoreDocumentEvents = false;
        }
    }

    public void setFullDocument(String text) {
        ignoreDocumentEvents = true;
        t_editor.setText(text);
        ignoreDocumentEvents = false;
    }

    //커서 하이라이트 보여주는 용도
    public void showRemoteCursor(String userId, int offset, int length) {
        try {
            Highlighter highlighter = t_editor.getHighlighter();

            // 이전 하이라이트 제거
            Object oldTag = cursorHighlights.get(userId);
            if (oldTag != null) {
                highlighter.removeHighlight(oldTag);
            }

            int start = offset;
            int end = offset + Math.max(1, length); // length가 0이면 한 글자만 강조

            Object tag = highlighter.addHighlight(
                    start, end,
                    new DefaultHighlighter.DefaultHighlightPainter(
                            new Color(12, 136, 231)  // 노란색 같은 공통 색
                    )
            );
            cursorHighlights.put(userId, tag);
        } catch (BadLocationException ignored) {}
    }

    // ===== 줄 잠금/해제 표시 (🔒 + 배경 하이라이트) =====
    public void lockLine(int lineIndex, String ownerUserId) {
        lockedLinesByOthers.add(lineIndex);

        try {
            int startOffset = t_editor.getLineStartOffset(lineIndex);
            int endOffset = t_editor.getLineEndOffset(lineIndex);

            Highlighter highlighter = t_editor.getHighlighter();

            // 기존 하이라이트 제거
            Object oldTag = lockHighlights.get(lineIndex);
            if (oldTag != null) {
                highlighter.removeHighlight(oldTag);
            }

            // 연한 빨간색 정도로 줄 전체 하이라이트
            Object tag = highlighter.addHighlight(
                    startOffset,
                    endOffset,
                    new DefaultHighlighter.DefaultHighlightPainter(
                            new Color(255, 220, 220)
                    )
            );
            lockHighlights.put(lineIndex, tag);

            // 상태바에 이모지로 잠금 표시
            l_mode.setText("모드: TEXT  🔒 line " + (lineIndex + 1) + " (" + ownerUserId + ")");
        } catch (BadLocationException ignored) {}
    }

    public void unlockLine(int lineIndex) {
        lockedLinesByOthers.remove(lineIndex);

        Object tag = lockHighlights.remove(lineIndex);
        if (tag != null) {
            t_editor.getHighlighter().removeHighlight(tag);
        }

        // 잠금 해제되면 기본 모드 텍스트로 복구 (필요하면 더 똑똑하게 바뀌게 가능)
        l_mode.setText("모드: TEXT");
    }




    //setter 메서드 (컨트롤러 주입)
    public void setController(EditorController controller) {
        this.controller = controller;
        registerDocumentListener(); //문서 입력,삭제 관련 리스너
        registerCaretListener(); // 커서 관련 리스너
    }
}
