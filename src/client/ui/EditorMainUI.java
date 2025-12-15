package client.ui;

import client.controller.EditorController;
import client.ui.style.UIStyle;
import global.object.DocumentMeta;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.util.List;

public class EditorMainUI extends JFrame {

    private EditorController controller;

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
    private Integer lastKeyPressLineIndex = null;

    private JPanel p_topBar;
    private JPanel p_statusBar;
    private JPanel p_sidebar;
    private JPanel p_editor;

    public EditorMainUI() {
        super("NoteSwing Client");

        UIStyle.applyFrame(this);

        buildGUI();

        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void buildGUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIStyle.BG);

        add(createTopBarPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createStatusBarPanel(), BorderLayout.SOUTH);
    }

    private JPanel createTopBarPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p_topBar = p;

        p.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        p.setBackground(UIStyle.PANEL);

        JPanel p_left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p_left.setOpaque(false);

        JLabel l_appName = new JLabel("NoteSwing");
        l_appName.setFont(l_appName.getFont().deriveFont(Font.BOLD, 18f));
        l_appName.setForeground(UIStyle.TEXT);

        l_docTitle = new JLabel(" / (no document)");
        l_docTitle.setForeground(UIStyle.SUB);

        p_left.add(l_appName);
        p_left.add(l_docTitle);

        JPanel p_right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        p_right.setOpaque(false);

        p.add(p_left, BorderLayout.WEST);
        p.add(p_right, BorderLayout.EAST);

        UIStyle.applyCard(p);
        return p;
    }

    private JComponent createCenterPanel() {
        p_sidebar = createSidebarPanel();
        p_editor = createEditorPanel();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, p_sidebar, p_editor);
        split.setDividerLocation(260);
        split.setOneTouchExpandable(true);
        split.setBackground(UIStyle.BG);

        return split;
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIStyle.BORDER));
        sidebar.setBackground(UIStyle.PANEL);

        sidebar.add(createSidebarHeader(), BorderLayout.NORTH);
        sidebar.add(createDocListScroll(), BorderLayout.CENTER);
        sidebar.add(createSidebarButtons(), BorderLayout.SOUTH);

        return sidebar;
    }

    private JPanel createSidebarHeader() {
        JPanel sideHeader = new JPanel(new BorderLayout());
        sideHeader.setBackground(UIStyle.PANEL);
        sideHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIStyle.BORDER),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel l_sideTitle = new JLabel("문서");
        l_sideTitle.setForeground(UIStyle.TEXT);
        l_sideTitle.setFont(l_sideTitle.getFont().deriveFont(Font.BOLD, 13f));

        JLabel l_sideHint = new JLabel("클릭해서 문서를 엽니다");
        l_sideHint.setForeground(UIStyle.SUB);

        sideHeader.add(l_sideTitle, BorderLayout.NORTH);
        sideHeader.add(l_sideHint, BorderLayout.SOUTH);

        return sideHeader;
    }

    private JScrollPane createDocListScroll() {
        docModel = new DefaultListModel<>();
        list_docs = new JList<>(docModel);
        list_docs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        list_docs.setBackground(Color.WHITE);
        list_docs.setForeground(UIStyle.TEXT);
        list_docs.setSelectionBackground(new Color(0xE9EDFF));
        list_docs.setSelectionForeground(UIStyle.TEXT);
        list_docs.setFixedCellHeight(44);

        list_docs.setCellRenderer(createDocListRenderer());
        list_docs.addListSelectionListener(e -> onDocSelected(e.getValueIsAdjusting()));

        JScrollPane docScroll = new JScrollPane(list_docs);
        docScroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER));
        docScroll.getViewport().setBackground(Color.WHITE);
        return docScroll;
    }

    private DefaultListCellRenderer createDocListRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus
            ) {
                JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                c.setText(value == null ? "" : value.toString());
                c.setForeground(UIStyle.TEXT);

                if (isSelected) {
                    c.setBackground(new Color(0xE9EDFF));
                    c.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 4, 0, 0, UIStyle.PRIMARY),
                            new EmptyBorder(6, 8, 6, 10)
                    ));
                } else {
                    c.setBackground(Color.WHITE);
                    c.setBorder(new EmptyBorder(6, 10, 6, 10));
                }
                return c;
            }
        };
    }

    private void onDocSelected(boolean valueIsAdjusting) {
        if (valueIsAdjusting) return;
        if (updatingDocList) return;
        if (controller == null) return;

        DocumentMeta meta = list_docs.getSelectedValue();
        if (meta == null || meta.id == null) return;
        if (meta.id.equals(currentDocId)) return;

        preClearForDocSwitch(meta.title);
        controller.openDocument(meta.id);
    }

    private JPanel createSidebarButtons() {
        JPanel p_btns = new JPanel(new GridLayout(1, 3, 6, 0));
        p_btns.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p_btns.setBackground(UIStyle.PANEL);

        b_newDoc = new JButton("새 문서");
        b_deleteDoc = new JButton("삭제");
        b_refresh = new JButton("새로고침");

        UIStyle.applyPrimaryButton(b_newDoc);
        UIStyle.applyDangerButton(b_deleteDoc);
        UIStyle.applyGhostButton(b_refresh);

        b_newDoc.addActionListener(e -> onCreateDoc());
        b_deleteDoc.addActionListener(e -> onDeleteDoc());
        b_refresh.addActionListener(e -> {
            if (controller != null) controller.requestDocList();
        });

        p_btns.add(b_newDoc);
        p_btns.add(b_deleteDoc);
        p_btns.add(b_refresh);

        return p_btns;
    }

    private void onCreateDoc() {
        if (controller == null) return;
        String title = JOptionPane.showInputDialog(this, "문서 제목을 입력하세요:", "새 문서", JOptionPane.PLAIN_MESSAGE);
        if (title == null) return;
        controller.createDocument(title);
    }

    private void onDeleteDoc() {
        if (controller == null) return;
        DocumentMeta meta = list_docs.getSelectedValue();
        if (meta == null || meta.id == null) return;

        int ok = JOptionPane.showConfirmDialog(
                this,
                "정말 삭제할까요?\n- " + meta.toString(),
                "문서 삭제",
                JOptionPane.YES_NO_OPTION
        );

        if (ok == JOptionPane.YES_OPTION) {
            controller.deleteDocument(meta.id);
        }
    }

    private JPanel createEditorPanel() {
        JPanel editor = new JPanel(new BorderLayout());
        editor.setBackground(UIStyle.PANEL);

        t_editor = new JTextPane();
        t_editor.setBackground(Color.WHITE);
        t_editor.setForeground(UIStyle.TEXT);
        t_editor.setCaretColor(UIStyle.PRIMARY);

        attachEditorKeyListeners();

        JScrollPane editorScroll = new JScrollPane(t_editor);
        editorScroll.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER));
        editorScroll.getViewport().setBackground(Color.WHITE);

        editor.add(editorScroll, BorderLayout.CENTER);

        textManager = new TextManager(t_editor);
        imageManager = new ImageManager(t_editor, textManager);

        return editor;
    }

    private void attachEditorKeyListeners() {
        t_editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                if (controller == null) return;

                int currentLine = getLineIndexFromCaretPosition();
                if (lastKeyPressLineIndex != null && lastKeyPressLineIndex != currentLine) {
                    controller.onLineSwitched(lastKeyPressLineIndex, currentLine);
                }
                lastKeyPressLineIndex = currentLine;
            }
        });

        t_editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                if (controller == null) return;

                char ch = e.getKeyChar();
                if (!Character.isISOControl(ch) || ch == '\n' || ch == '\b') {
                    int lineIndex = getLineIndexFromCaretPosition();
                    if (controller.isLineLockedByOther(lineIndex)) {
                        showLineLockedDialog(lineIndex);
                        e.consume();
                    }
                }
            }
        });
    }

    private JPanel createStatusBarPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p_statusBar = p;

        p.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        p.setBackground(UIStyle.PANEL);

        l_connectionStatus = new JLabel("서버 연결: 끊김");
        l_mode = new JLabel("모드: TEXT+IMAGE");

        l_connectionStatus.setForeground(UIStyle.SUB);
        l_mode.setForeground(UIStyle.SUB);

        p.add(l_connectionStatus, BorderLayout.WEST);
        p.add(l_mode, BorderLayout.EAST);

        UIStyle.applyCard(p);
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

        if (docModel.size() > 0 && list_docs.getSelectedIndex() == -1) {
            list_docs.setSelectedIndex(0);
        }
    }

    public int getLineIndexFromCaretPosition() {
        try {
            int caretPos = t_editor.getCaretPosition();
            Document doc = t_editor.getDocument();
            String text = doc.getText(0, caretPos);
            int line = 0;
            for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '\n') line++;
            return line;
        } catch (Exception e) {
            return 0;
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

    public void lockLine(int lineIndex, String ownerId) {
        textManager.highlightLine(lineIndex, new Color(255, 200, 200));
    }

    public void unlockLine(int lineIndex, String ownerId) {
        textManager.clearLineHighlight(lineIndex);
    }

    public void clearAllLineHighlights() {
        textManager.clearLineHighlight(-1);
    }

    public void showLineLockedDialog(int lineIndex) {
        JOptionPane.showMessageDialog(
                this,
                "현재 " + (lineIndex + 1) + "번째 줄은 다른 사용자가 편집 중이라 수정할 수 없습니다.",
                "편집 불가",
                JOptionPane.INFORMATION_MESSAGE
        );
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
