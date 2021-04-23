package net.minecraft;

import javax.swing.*;
import java.awt.*;

public class TransparentPanel extends JPanel
{
    private static final long serialVersionUID = 1L;
    private Insets insets;
    
    public TransparentPanel() {
    }
    
    public TransparentPanel(final LayoutManager layout) {
        this.setLayout(layout);
    }
    
    @Override
    public boolean isOpaque() {
        return false;
    }
    
    public void setInsets(final int a, final int b, final int c, final int d) {
        this.insets = new Insets(a, b, c, d);
    }
    
    @Override
    public Insets getInsets() {
        if (this.insets == null) {
            return super.getInsets();
        }
        return this.insets;
    }
}
