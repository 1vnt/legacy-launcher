package net.minecraft;

import javax.swing.*;
import java.awt.*;

public class TransparentCheckbox extends JCheckBox
{
    private static final long serialVersionUID = 1L;
    
    public TransparentCheckbox(final String string) {
        super(string);
        this.setForeground(Color.WHITE);
    }
    
    @Override
    public boolean isOpaque() {
        return false;
    }
}
