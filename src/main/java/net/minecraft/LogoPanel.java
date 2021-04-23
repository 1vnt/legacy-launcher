package net.minecraft;

import javax.swing.*;
import javax.imageio.*;
import java.io.*;
import java.awt.*;
import java.awt.image.*;

public class LogoPanel extends JPanel
{
    private static final long serialVersionUID = 1L;
    private Image bgImage;
    
    public LogoPanel() {
        this.setOpaque(true);
        try {
            final BufferedImage src = ImageIO.read(LoginForm.class.getResource("/logo.png"));
            final int w = src.getWidth();
            final int h = src.getHeight();
            this.bgImage = src.getScaledInstance(w, h, 16);
            this.setPreferredSize(new Dimension(w + 32, h + 32));
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
        g2.drawImage(this.bgImage, 24, 24, null);
    }
}
