package client.ui.image;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

public class ImageIOHelper {

    public static Dimension calcScaledSize(byte[] data, int maxW, int maxH) {
        Image img = Toolkit.getDefaultToolkit().createImage(data);
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        if (w <= 0 || h <= 0) {
            return new Dimension(200, 150); // fallback
        }
        double scale = Math.min((double) maxW / w, (double) maxH / h);
        if (scale >= 1.0) {
            return new Dimension(w, h);
        }
        return new Dimension((int) (w * scale), (int) (h * scale));
    }

    public static ImageIcon createScaledIcon(byte[] data, int w, int h) {
        Image img = Toolkit.getDefaultToolkit().createImage(data);
        Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    public static boolean isImageFile(File f) {
        String name = f.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") ||
                name.endsWith(".jpeg") || name.endsWith(".gif") ||
                name.endsWith(".bmp");
    }

    public static byte[] imageToPngBytes(Image img) throws Exception {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buffered.createGraphics();
        g2.drawImage(img, 0, 0, null);
        g2.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(buffered, "png", baos);
        return baos.toByteArray();
    }
}
