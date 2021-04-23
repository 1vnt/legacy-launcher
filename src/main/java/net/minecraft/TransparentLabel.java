package net.minecraft;

import javax.swing.*;
import java.awt.*;

public class TransparentLabel extends JLabel
{
    private static final long serialVersionUID = 1L;
    
    public TransparentLabel(final String string, final int center) {
        super(string, center);
        this.setForeground(Color.WHITE);
    }
    
    public TransparentLabel(final String string) {
        super(string);
        this.setForeground(Color.WHITE);
    }
    
    @Override
    public boolean isOpaque() {
        return false;
    }
}
