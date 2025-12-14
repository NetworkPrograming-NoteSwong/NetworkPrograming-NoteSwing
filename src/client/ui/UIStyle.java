package client.ui;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;

public final class UIStyle {
    private UIStyle() {}

    public static final Color BG = new Color(0xF5F6FA);
    public static final Color PANEL = Color.WHITE;
    public static final Color BORDER = new Color(0xE3E6EF);

    public static final Color TEXT = new Color(0x111827);
    public static final Color SUB = new Color(0x6B7280);

    public static final Color PRIMARY = new Color(0x4F46E5);
    public static final Color PRIMARY_HOVER = new Color(0x4338CA);

    public static final Color DANGER = new Color(0xEF4444);
    public static final Color DANGER_HOVER = new Color(0xDC2626);

    public static Font baseFont() { return new Font("Dialog", Font.PLAIN, 12); }
    public static Font boldFont() { return new Font("Dialog", Font.BOLD, 12); }
    public static final Font GLOBAL_FONT = new Font("Malgun Gothic", Font.PLAIN, 14);


    public static void applyFrame(JFrame f) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        setGlobalUIFont(GLOBAL_FONT);
        f.getContentPane().setBackground(BG);
    }

    public static void applyPrimaryButton(JButton b) {
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(true);
        b.setOpaque(true);

        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setFont(boldFont());
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new RoundedBorder(12, PRIMARY));

        installHover(b, PRIMARY, PRIMARY_HOVER);
        b.setMargin(new Insets(10, 14, 10, 14));
    }

    public static void applyDangerButton(JButton b) {
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(true);
        b.setOpaque(true);

        b.setBackground(DANGER);
        b.setForeground(Color.WHITE);
        b.setFont(boldFont());
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new RoundedBorder(12, DANGER));

        installHover(b, DANGER, DANGER_HOVER);
        b.setMargin(new Insets(10, 14, 10, 14));
    }

    public static void applyGhostButton(JButton b) {
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(true);
        b.setOpaque(true);

        b.setBackground(new Color(0xF3F4F6));
        b.setForeground(TEXT);
        b.setFont(boldFont());
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new RoundedBorder(12, BORDER));

        installHover(b, new Color(0xF3F4F6), new Color(0xE5E7EB));
        b.setMargin(new Insets(10, 14, 10, 14));
    }

    public static void applyCard(JComponent p) {
        p.setOpaque(true);
        p.setBackground(PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(16, BORDER),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
    }

    private static void installHover(JButton b, Color normal, Color hover) {
        b.addChangeListener(e -> {
            ButtonModel m = b.getModel();
            if (!b.isEnabled()) return;
            if (m.isRollover()) b.setBackground(hover);
            else b.setBackground(normal);
        });
    }

    public static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        public RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(8, 10, 8, 10);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = 10; insets.right = 10; insets.top = 8; insets.bottom = 8;
            return insets;
        }
    }

    private static void setGlobalUIFont(Font font) {
        for (Object key : UIManager.getLookAndFeelDefaults().keySet()) {
            Object value = UIManager.get(key);
            if (value instanceof Font) {
                UIManager.put(key, font);
            }
        }
    }

}


