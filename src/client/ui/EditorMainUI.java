package client.ui;

import client.controller.EditorController;
import global.object.DocumentMeta;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class EditorMainUI extends JFrame {

    private EditorController controller;

    private JLabel l_loginStatus;
    private JButton b_login;
    private JButton b_logout;

    private JLabel l_docTitle;

    private JList<DocumentMeta> list_docs;
    private DefaultListModel<DocumentMeta> docModel;
    private boolean updatingDocList = false;

    private JButton b_newDoc;
    private JButton b_deleteDoc;
    private JButton b_refresh;

    private JTextPane t_editor;
    private TextManager textManager;
    private ImageManager imageManager;

    private JLabel l_connectionStatus;
    private JLabel l_mode;

    private String currentDocId = null;

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
        l_docTitle = new JLabel(" / (no document)");
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
        p_sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 220, 220)));

        JLabel l_sideTitle = new JLabel("문서");
        l_sideTitle.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p_sidebar.add(l_sideTitle, BorderLayout.NORTH);

        docModel = new DefaultListModel<>();
        list_docs = new JList<>(docModel);
        list_docs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        list_docs.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            if (updatingDocList) return;
            if (controller == null) return;

            DocumentMeta meta = list_docs.getSelectedValue();
            if (meta == null || meta.id == null) return;
            if (meta.id.equals(currentDocId)) return;

            preClearForDocSwitch(meta.title);
            controller.openDocument(meta.id);
        });

        p_sidebar.add(new JScrollPane(list_docs), BorderLayout.CENTER);

        // 버튼 바
        JPanel p_btns = new JPanel(new GridLayout(1, 3, 6, 0));
        p_btns.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        b_newDoc = new JButton("새 문서");
        b_deleteDoc = new JButton("삭제");
        b_refresh = new JButton("새로고침");

        b_newDoc.addActionListener(e -> {
            if (controller == null) return;
            String title = JOptionPane.showInputDialog(this, "문서 제목을 입력하세요:", "새 문서", JOptionPane.PLAIN_MESSAGE);
            if (title == null) return;
            controller.createDocument(title);
        });

        b_deleteDoc.addActionListener(e -> {
            if (controller == null) return;
            DocumentMeta meta = list_docs.getSelectedValue();
            if (meta == null || meta.id == null) return;

            int ok = JOptionPane.showConfirmDialog(this,
                    "정말 삭제할까요?\n- " + meta.toString(),
                    "문서 삭제", JOptionPane.YES_NO_OPTION);

            if (ok == JOptionPane.YES_OPTION) {
                controller.deleteDocument(meta.id);
            }
        });

        b_refresh.addActionListener(e -> {
            if (controller == null) return;
            controller.requestDocList();
        });

        p_btns.add(b_newDoc);
        p_btns.add(b_deleteDoc);
        p_btns.add(b_refresh);

        p_sidebar.add(p_btns, BorderLayout.SOUTH);

        // 에디터
        JPanel p_editor = new JPanel(new BorderLayout());
        t_editor = new JTextPane();
        t_editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        p_editor.add(new JScrollPane(t_editor), BorderLayout.CENTER);

        textManager = new TextManager(t_editor);
        imageManager = new ImageManager(t_editor, textManager);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, p_sidebar, p_editor);
        split.setDividerLocation(260);
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

    private void preClearForDocSwitch(String title) {
        imageManager.clearAll();
        textManager.setFullDocument("");
        setDocTitle(title);
    }

    public void setDocumentList(List<DocumentMeta> docs) {
        updatingDocList = true;
        try {
            docModel.clear();
            if (docs != null) {
                for (DocumentMeta m : docs) docModel.addElement(m);
            }

            // 현재 문서가 목록에 있으면 유지 선택
            if (currentDocId != null) {
                for (int i = 0; i < docModel.size(); i++) {
                    DocumentMeta m = docModel.get(i);
                    if (m != null && currentDocId.equals(m.id)) {
                        list_docs.setSelectedIndex(i);
                        return;
                    }
                }
            }

        } finally {
            updatingDocList = false;
        }

        // 아무것도 선택 안돼있으면 첫 문서 자동 선택(→ DOC_OPEN 발생)
        if (docModel.size() > 0 && list_docs.getSelectedIndex() == -1) {
            list_docs.setSelectedIndex(0);
        }
    }

    public void onSnapshotFullSync(String docId, String title, String text) {
        this.currentDocId = docId;
        setDocTitle(title);

        imageManager.clearAll();
        textManager.setFullDocument(text == null ? "" : text);
    }

    private void setDocTitle(String title) {
        String t = (title == null || title.isBlank()) ? "Untitled" : title;
        l_docTitle.setText(" / " + t);
    }

    public void updateLoginStatus(String text) { l_loginStatus.setText(text); }
    public void updateConnectionStatus(String text) { l_connectionStatus.setText(text); }

    public void applyInsert(int offset, String text) {
        textManager.applyInsert(offset, text);
        imageManager.onTextInserted(offset, text == null ? 0 : text.length());
    }

    public void applyDelete(int offset, int length) {
        textManager.applyDelete(offset, length);
        imageManager.onTextDeleted(offset, length);
    }

    public void applyImageInsert(int blockId, int offset, int width, int height, byte[] data) {
        imageManager.insertImageRemote(blockId, offset, width, height, data);
    }

    public void applyImageResize(int blockId, int width, int height) {
        imageManager.applyRemoteResize(blockId, width, height);
    }

    public void applyImageMove(int blockId, int newOffset) {
        imageManager.applyRemoteMove(blockId, newOffset);
    }

    public void setController(EditorController controller) {
        this.controller = controller;

        textManager.setChangeListener(new TextManager.DocumentChangeListener() {
            @Override
            public void onTextInserted(int offset, String text) {
                controller.onTextInserted(offset, text);
                imageManager.onTextInserted(offset, text.length());
            }

            @Override
            public void onTextDeleted(int offset, int length) {
                controller.onTextDeleted(offset, length);
                imageManager.onTextDeleted(offset, length);
            }

            @Override
            public void onFullDocumentChanged(String text) {
                controller.onFullDocumentChanged(text);
            }
        });
        textManager.registerListener();

        imageManager.setEventListener(new ImageEventListener() {
            @Override
            public void onLocalImageInserted(int blockId, int offset, int width, int height, byte[] data) {
                controller.onLocalImageInserted(blockId, offset, width, height, data);
            }

            @Override
            public void onLocalImageResized(int blockId, int width, int height) {
                controller.onLocalImageResized(blockId, width, height);
            }

            @Override
            public void onLocalImageMoved(int blockId, int newOffset) {
                controller.onLocalImageMoved(blockId, newOffset);
            }
        });
    }
}
