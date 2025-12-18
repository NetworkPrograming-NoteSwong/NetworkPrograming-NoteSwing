package client.ui;

import client.controller.EditorController;
import global.object.DocumentMeta;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class LobbyUI extends JFrame {

    private final EditorController controller;

    private final DefaultListModel<DocumentMeta> allModel = new DefaultListModel<>();
    private final DefaultListModel<DocumentMeta> myModel  = new DefaultListModel<>();
    private final JList<DocumentMeta> listAll = new JList<>(allModel);
    private final JList<DocumentMeta> listMy  = new JList<>(myModel);

    private final JLabel lStatus = new JLabel("서버 연결: 대기중");

    public LobbyUI(EditorController controller) {
        super("NoteSwing Lobby");
        this.controller = controller;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(topBar(), BorderLayout.NORTH);
        add(center(), BorderLayout.CENTER);
        add(bottom(), BorderLayout.SOUTH);

        setVisible(true);
    }

    private JComponent topBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel title = new JLabel("NoteSwing");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        p.add(title, BorderLayout.WEST);
        p.add(lStatus, BorderLayout.EAST);
        return p;
    }

    private JComponent center() {
        JPanel root = new JPanel(new GridLayout(1, 2, 12, 0));
        root.setBorder(new EmptyBorder(10, 12, 10, 12));

        root.add(wrap("전체 문서(서버)", listAll));
        root.add(wrap("내 협업 방(선택)", listMy));

        listAll.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listMy.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        return root;
    }

    private JPanel wrap(String title, JList<DocumentMeta> list) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        JLabel l = new JLabel(title);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
        p.add(l, BorderLayout.NORTH);
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        return p;
    }

    private JComponent bottom() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(10, 12, 12, 12));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton bAdd = new JButton("내 방에 추가 →");
        JButton bRemove = new JButton("내 방에서 제거");
        JButton bRefresh = new JButton("새로고침");
        left.add(bAdd);
        left.add(bRemove);
        left.add(bRefresh);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton bCreate = new JButton("새 문서 만들기");
        JButton bEnter = new JButton("입장");
        right.add(bCreate);
        right.add(bEnter);

        bAdd.addActionListener(e -> addSelectedFromAll());
        bRemove.addActionListener(e -> removeSelectedFromMy());
        bRefresh.addActionListener(e -> controller.requestDocList());

        bCreate.addActionListener(e -> {
            String title = JOptionPane.showInputDialog(this, "문서 제목:", "새 문서", JOptionPane.PLAIN_MESSAGE);
            if (title != null && !title.isBlank()) controller.createDocument(title);
        });

        bEnter.addActionListener(e -> enterSelected());

        p.add(left, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private void addSelectedFromAll() {
        DocumentMeta m = listAll.getSelectedValue();
        if (m == null || m.id == null) return;
        controller.addMyDoc(m.id);
    }

    private void removeSelectedFromMy() {
        DocumentMeta m = listMy.getSelectedValue();
        if (m == null || m.id == null) return;
        controller.removeMyDoc(m.id);
    }

    private void enterSelected() {
        DocumentMeta m = listMy.getSelectedValue();
        if (m == null || m.id == null) {
            JOptionPane.showMessageDialog(this, "입장할 방(문서)을 선택하세요.", "안내", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        controller.enterWorkspace(null, m.id); // myDocIds는 controller가 유지
    }

    public void updateConnectionStatus(String text) {
        lStatus.setText(text == null ? "" : text);
    }

    public void setAllDocs(List<DocumentMeta> docs) {
        allModel.clear();
        List<DocumentMeta> safe = (docs == null) ? new ArrayList<>() : docs;
        for (DocumentMeta m : safe) allModel.addElement(m);
    }

    public void setMyDocs(List<DocumentMeta> myDocs) {
        myModel.clear();
        List<DocumentMeta> safe = (myDocs == null) ? new ArrayList<>() : myDocs;
        for (DocumentMeta m : safe) myModel.addElement(m);
    }

    public void closeLobby() {
        setVisible(false);
        dispose();
    }
}
