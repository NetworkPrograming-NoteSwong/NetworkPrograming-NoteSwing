package client.ui;

import client.controller.EditorController;

import javax.swing.*;
import java.awt.*;

/**
 * UI 렌더링 및 레이아웃 담당
 */
public class EditorMainUI extends JFrame {

    private EditorController controller;

    // 상단 바
    private JLabel l_loginStatus;
    private JButton b_login;
    private JButton b_logout;

    // 왼쪽 문서 리스트
    private JList<String> list_docs;

    // 중앙 에디터
    private JTextPane t_editor;
    private TextManager textManager;
    private ImageManager imageManager;

    // 하단 상태바
    private JLabel l_connectionStatus;
    private JLabel l_mode;

    public EditorMainUI() {
        super("NoteSwing Client");

        buildGUI();

        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void buildGUI() {
        setLayout(new BorderLayout());

        add(createTopBarPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createStatusBarPanel(), BorderLayout.SOUTH);
    }

    private JPanel createTopBarPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        p.setBackground(new Color(245, 245, 245));

        JPanel p_left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JLabel l_appName = new JLabel("NoteSwing");
        l_appName.setFont(l_appName.getFont().deriveFont(Font.BOLD, 18f));
        JLabel l_docTitle = new JLabel(" / Untitled Document");

        p_left.add(l_appName);
        p_left.add(l_docTitle);

        JPanel p_right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        l_loginStatus = new JLabel("로그인되지 않음");
        b_login = new JButton("로그인");
        b_logout = new JButton("로그아웃");
        b_logout.setEnabled(false);

        p_right.add(l_loginStatus);
        p_right.add(b_login);
        p_right.add(b_logout);

        p.add(p_left, BorderLayout.WEST);
        p.add(p_right, BorderLayout.EAST);

        return p;
    }

    private JComponent createCenterPanel() {
        JPanel p_sidebar = new JPanel(new BorderLayout());
        p_sidebar.setBorder(BorderFactory.createMatteBorder(
                0, 0, 0, 1, new Color(220, 220, 220)));

        JLabel l_sideTitle = new JLabel("문서");
        l_sideTitle.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p_sidebar.add(l_sideTitle, BorderLayout.NORTH);

        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("Untitled Document");
        model.addElement("Project Plan");
        model.addElement("README.md");

        list_docs = new JList<>(model);
        p_sidebar.add(new JScrollPane(list_docs), BorderLayout.CENTER);

        JPanel p_editor = new JPanel(new BorderLayout());
        t_editor = new JTextPane();
        t_editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        p_editor.add(new JScrollPane(t_editor), BorderLayout.CENTER);

        // 텍스트/이미지 매니저 초기화
        textManager = new TextManager(t_editor);
        imageManager = new ImageManager(t_editor, textManager);

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                p_sidebar,
                p_editor
        );
        split.setDividerLocation(220);
        split.setOneTouchExpandable(true);

        return split;
    }

    private JPanel createStatusBarPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        p.setBackground(new Color(250, 250, 250));

        l_connectionStatus = new JLabel("서버 연결: 끊김");
        l_mode = new JLabel("모드: TEXT+IMAGE");

        p.add(l_connectionStatus, BorderLayout.WEST);
        p.add(l_mode, BorderLayout.EAST);

        return p;
    }

    public void updateLoginStatus(String text) {
        l_loginStatus.setText(text);
    }

    public void updateConnectionStatus(String text) {
        l_connectionStatus.setText(text);
    }

    // 서버 텍스트 메시지 처리
    public void applyInsert(int offset, String text) {
        textManager.applyInsert(offset, text);
    }

    public void applyDelete(int offset, int length) {
        textManager.applyDelete(offset, length);
    }

    public void setFullDocument(String text) {
        textManager.setFullDocument(text);
    }

    // 서버 이미지 메시지 처리
    public void applyImageInsert(int offset, int blockId, byte[] payload) {
        imageManager.insertImageRemote(offset, payload);
    }

    // Controller 주입
    public void setController(EditorController controller) {
        this.controller = controller;

        imageManager.setController(controller);

        textManager.setChangeListener(new TextManager.DocumentChangeListener() {
            @Override
            public void onTextInserted(int offset, String text) {
                controller.onTextInserted(offset, text);
            }

            @Override
            public void onTextDeleted(int offset, int length) {
                controller.onTextDeleted(offset, length);
            }

            @Override
            public void onFullDocumentChanged(String text) {
                // 현재는 사용하지 않음
            }
        });

        textManager.registerListener();
    }
}
