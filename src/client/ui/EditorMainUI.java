// src/client/ui/EditorMainUI.java
package client.ui;

import client.controller.EditorController;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

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

    public EditorMainUI() {
        super("NoteSwing Client");

        buildGUI();

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
                //추후에 구현
            }
        });
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


    //setter 메서드 (컨트롤러 주입)
    public void setController(EditorController controller) {
        this.controller = controller;
        registerDocumentListener();
    }
}
