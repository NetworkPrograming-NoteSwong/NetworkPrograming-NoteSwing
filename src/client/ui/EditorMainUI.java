// src/client/ui/EditorMainUI.java
package client.ui;

import client.controller.EditorController;
import server.core.DocumentManager; // IMAGE_PLACEHOLDER와 맞추기용 (상수값만 공유)

// Swing / 텍스트 관련
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;

// 유틸
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditorMainUI extends JFrame {

    // 이미지 플레이스홀더 문자 (서버 DocumentManager와 동일 값 사용)
    private static final char IMAGE_PLACEHOLDER = DocumentManager.IMAGE_PLACEHOLDER;

    // 컨트롤러
    private EditorController controller;

    // 상단 바 컴포넌트
    private JLabel l_loginStatus;
    private JButton b_login;
    private JButton b_logout;

    // 왼쪽 문서 리스트
    private JList<String> list_docs;

    // 중앙 코드 에디터
    private JTextPane t_editor;
    private JScrollPane editorScrollPane;

    // 하단 상태바
    private JLabel l_connectionStatus;
    private JLabel l_mode;

    // Document 이벤트 플래그 변수
    private boolean ignoreDocumentEvents = false;

    // 커서 하이라이트로 표시
    private final Map<String, Object> cursorHighlights = new HashMap<>();

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
        // b_login.addActionListener(e -> controller.onClickLogin());
        // b_logout.addActionListener(e -> controller.onClickLogout());

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

        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("Untitled Document");
        model.addElement("Project Plan");
        model.addElement("README.md");

        list_docs = new JList<>(model);
        p_sidebar.add(new JScrollPane(list_docs), BorderLayout.CENTER);

        // TODO: 문서 선택 이벤트도 나중에 컨트롤러에 연결

        JPanel p_editor = new JPanel(new BorderLayout());
        t_editor = new JTextPane();
        t_editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        editorScrollPane = new JScrollPane(t_editor);
        p_editor.add(editorScrollPane, BorderLayout.CENTER);

        // 이미지 드롭/붙여넣기 핸들러 등록
        setupImageTransferHandler();

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

    // 내가 직접 타이핑/삭제한 변경을 감지해서 컨트롤러에 알려주는 역할
    private void registerDocumentListener() {
        t_editor.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                if (ignoreDocumentEvents) return;

                try {
                    int offset = e.getOffset();
                    int length = e.getLength();
                    String inserted = t_editor.getText().substring(offset, offset + length);
                    // 이미지 플레이스홀더 같은 것도 문자열에 포함될 수 있음
                    controller.onTextInserted(offset, inserted);
                } catch (Exception ignored) {
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (ignoreDocumentEvents) return;
                controller.onTextDeleted(e.getOffset(), e.getLength());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (ignoreDocumentEvents) return;
                // StyledDocument에서 속성 변경 시 호출됨 (아이콘 삽입 등은 ignore 플래그로 무시)
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

    // 다른 사용자가 편집한 결과를 우리 에디터에 반영할 때만 쓰는 메서드(밑에 3개)
    public void applyInsert(int offset, String text) {
        ignoreDocumentEvents = true;
        try {
            t_editor.getDocument().insertString(offset, text, null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        } finally {
            ignoreDocumentEvents = false;
        }
    }

    public void applyDelete(int offset, int length) {
        ignoreDocumentEvents = true;
        try {
            t_editor.getDocument().remove(offset, length);
        } catch (BadLocationException e) {
            e.printStackTrace();
        } finally {
            ignoreDocumentEvents = false;
        }
    }

    public void setFullDocument(String text) {
        ignoreDocumentEvents = true;
        try {
            t_editor.setText(text != null ? text : "");
        } finally {
            ignoreDocumentEvents = false;
        }
    }

    // 커서 하이라이트 보여주는 용도
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
                            new Color(255, 255, 150)  // 노란색 같은 공통 색
                    )
            );
            cursorHighlights.put(userId, tag);
        } catch (BadLocationException ignored) {
        }
    }

    // ===== 이미지 삽입/표시 =====

    /**
     * 서버/다른 클라이언트에서 IMAGE_INSERT를 받았을 때 호출됨
     */
    public void applyImageInsert(int blockId, int offset, byte[] data, int width, int height) {
        ignoreDocumentEvents = true;
        try {
            insertImageIntoDocument(blockId, offset, data, width, height);
        } finally {
            ignoreDocumentEvents = false;
        }
    }

    /**
     * 서버/다른 클라이언트에서 IMAGE_RESIZE를 받았을 때 호출됨
     */
    public void applyImageResize(int blockId, int newWidth, int newHeight) {
        ignoreDocumentEvents = true;
        try {
            StyledDocument doc = t_editor.getStyledDocument();
            int len = doc.getLength();
            for (int i = 0; i < len; i++) {
                Element elem = doc.getCharacterElement(i);
                Object bid = elem.getAttributes().getAttribute("blockId");
                if (bid instanceof Integer && ((Integer) bid) == blockId) {
                    Icon icon = StyleConstants.getIcon(elem.getAttributes());
                    if (icon instanceof ImageIcon imageIcon) {
                        Image original = imageIcon.getImage();
                        Image scaled = original.getScaledInstance(
                                newWidth, newHeight, Image.SCALE_SMOOTH);
                        ImageIcon newIcon = new ImageIcon(scaled);

                        SimpleAttributeSet attrs = new SimpleAttributeSet();
                        StyleConstants.setIcon(attrs, newIcon);
                        attrs.addAttribute("blockId", blockId);

                        doc.setCharacterAttributes(i, 1, attrs, true);
                    }
                    break;
                }
            }
        } finally {
            ignoreDocumentEvents = false;
        }
    }

    /**
     * 실제 문서에 이미지 아이콘을 삽입하는 공통 메서드.
     * - offset 위치에 IMAGE_PLACEHOLDER 문자를 삽입 (필요 시)
     * - 해당 위치의 문자에 icon + blockId 속성을 부여.
     */
    private void insertImageIntoDocument(int blockId, int offset, byte[] data, int width, int height) {
        try {
            StyledDocument doc = t_editor.getStyledDocument();
            int docLen = doc.getLength();

            // offset 보정
            if (offset < 0) offset = 0;
            if (offset > docLen) offset = docLen;

            // 바이트 → BufferedImage
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) return;

            // 실제 표시 크기로 스케일
            Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            ImageIcon icon = new ImageIcon(scaled);

            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setIcon(attrs, icon);
            attrs.addAttribute("blockId", blockId);

            boolean placeholderExists = false;
            if (offset < doc.getLength()) {
                String ch = doc.getText(offset, 1);
                if (!ch.isEmpty() && ch.charAt(0) == IMAGE_PLACEHOLDER) {
                    placeholderExists = true;
                }
            }

            if (placeholderExists) {
                // 이미 서버 DocumentManager가 넣어둔 플레이스홀더가 있는 경우:
                // 문자 자체는 그대로 두고 속성만 덮어쓴다.
                doc.setCharacterAttributes(offset, 1, attrs, true);
            } else {
                // 플레이스홀더 없이 처음 삽입하는 경우:
                doc.insertString(offset, String.valueOf(IMAGE_PLACEHOLDER), attrs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== 로컬에서 드롭/붙여넣기로 이미지 넣기 =====

    private void setupImageTransferHandler() {
        t_editor.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                if (support.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                    return true;
                }
                if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return true;
                }
                if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    // 텍스트 붙여넣기도 허용
                    return true;
                }
                return false;
            }

            @Override
            public boolean importData(TransferSupport support) {
                try {
                    if (!canImport(support)) return false;

                    // 이미지 드롭/붙여넣기
                    if (support.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                        Transferable t = support.getTransferable();
                        Image img = (Image) t.getTransferData(DataFlavor.imageFlavor);
                        if (img instanceof BufferedImage bi) {
                            insertLocalImage(bi);
                        } else {
                            // BufferedImage로 변환
                            BufferedImage bi2 = new BufferedImage(
                                    img.getWidth(null),
                                    img.getHeight(null),
                                    BufferedImage.TYPE_INT_ARGB
                            );
                            Graphics2D g2 = bi2.createGraphics();
                            g2.drawImage(img, 0, 0, null);
                            g2.dispose();
                            insertLocalImage(bi2);
                        }
                        return true;
                    }

                    // 파일 드롭 (이미지 파일인 경우)
                    if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        Transferable t = support.getTransferable();
                        @SuppressWarnings("unchecked")
                        List<java.io.File> files =
                                (List<java.io.File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                        for (java.io.File f : files) {
                            try {
                                BufferedImage bi = ImageIO.read(f);
                                if (bi != null) {
                                    insertLocalImage(bi);
                                }
                            } catch (Exception ignored) { }
                        }
                        return true;
                    }

                    // 일반 텍스트 붙여넣기
                    if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        String s = (String) support.getTransferable()
                                .getTransferData(DataFlavor.stringFlavor);
                        t_editor.replaceSelection(s);
                        return true;
                    }

                    return false;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        });
    }

    /**
     * 로컬에서 이미지 하나를 삽입할 때 호출.
     * - 에디터의 현재 caret 위치 기준
     * - 적당한 크기로 스케일 후
     * - 서버로 IMAGE_INSERT 전송 + 문서에 즉시 반영
     */
    private void insertLocalImage(BufferedImage image) {
        if (controller == null || image == null) return;

        int caret = t_editor.getCaretPosition();
        Dimension size = computeInitialImageSize(image);
        byte[] bytes = encodeImageToPng(image);

        if (bytes == null) return;

        // 서버 쪽에 IMAGE_INSERT 전송 (blockId 생성은 컨트롤러에서)
        int blockId = controller.onImageInserted(caret, bytes, size.width, size.height);

        // 로컬 문서에도 즉시 반영
        ignoreDocumentEvents = true;
        try {
            insertImageIntoDocument(blockId, caret, bytes, size.width, size.height);
        } finally {
            ignoreDocumentEvents = false;
        }
    }

    private Dimension computeInitialImageSize(BufferedImage image) {
        int originalW = image.getWidth();
        int originalH = image.getHeight();

        // 에디터 뷰포트 기준으로 최대 폭 계산
        int maxWidth = 600;
        if (editorScrollPane != null && editorScrollPane.getViewport().getWidth() > 0) {
            maxWidth = (int) (editorScrollPane.getViewport().getWidth() * 0.7);
        }

        double scale = 1.0;
        if (originalW > maxWidth) {
            scale = (double) maxWidth / (double) originalW;
        }

        int w = (int) Math.max(50, originalW * scale);
        int h = (int) Math.max(50, originalH * scale);
        return new Dimension(w, h);
    }

    private byte[] encodeImageToPng(BufferedImage image) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bos);
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    // setter 메서드 (컨트롤러 주입)
    public void setController(EditorController controller) {
        this.controller = controller;
        registerDocumentListener(); // 문서 입력,삭제 관련 리스너
        registerCaretListener(); // 커서 관련 리스너
    }
}