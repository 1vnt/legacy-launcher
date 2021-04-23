package net.minecraft;

import javax.swing.*;

public class TransparentButton extends JButton
{
    private static final long serialVersionUID = 1L;
    
    public TransparentButton(final String string) {
        super(string);
    }
    
    @Override
    public boolean isOpaque() {
        return false;
    }
}
