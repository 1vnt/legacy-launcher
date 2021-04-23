package net.minecraft;

import javax.swing.*;
import javax.imageio.*;
import java.io.*;
import java.awt.geom.*;
import java.awt.*;

public class TexturedPanel extends JPanel
{
    private static final long serialVersionUID = 1L;
    private Image img;
    private Image bgImage;
    
    public TexturedPanel() {
        this.setOpaque(true);
        try {
            this.bgImage = ImageIO.read(LoginForm.class.getResource("/dirt.png")).getScaledInstance(32, 32, 16);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void update(final Graphics g) {
        this.paint(g);
    }
    
    public void paintComponent(final Graphics g2) {
        final int w = this.getWidth() / 2 + 1;
        final int h = this.getHeight() / 2 + 1;
        if (this.img == null || this.img.getWidth(null) != w || this.img.getHeight(null) != h) {
            this.img = this.createImage(w, h);
            final Graphics g3 = this.img.getGraphics();
            for (int x = 0; x <= w / 32; ++x) {
                for (int y = 0; y <= h / 32; ++y) {
                    g3.drawImage(this.bgImage, x * 32, y * 32, null);
                }
            }
            if (g3 instanceof Graphics2D) {
                final Graphics2D gg = (Graphics2D)g3;
                int gh = 1;
                gg.setPaint(new GradientPaint(new Point2D.Float(0.0f, 0.0f), new Color(553648127, true), new Point2D.Float(0.0f, (float)gh), new Color(0, true)));
                gg.fillRect(0, 0, w, gh);
                gh = h;
                gg.setPaint(new GradientPaint(new Point2D.Float(0.0f, 0.0f), new Color(0, true), new Point2D.Float(0.0f, (float)gh), new Color(1610612736, true)));
                gg.fillRect(0, 0, w, gh);
            }
            g3.dispose();
        }
        g2.drawImage(this.img, 0, 0, w * 2, h * 2, null);
    }
}
