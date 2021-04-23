package net.minecraft;


import javax.swing.border.*;
import javax.swing.*;
import java.net.*;
import java.awt.event.*;
import java.awt.*;

public class OptionsPanel extends JDialog {
    private static final long serialVersionUID = 1L;

    public OptionsPanel(final Frame parent) {
        super(parent);
        this.setModal(true);
        final JPanel panel = new JPanel(new BorderLayout());
        final JLabel label = new JLabel("Launcher options", 0);
        label.setBorder(new EmptyBorder(0, 0, 16, 0));
        label.setFont(new Font("Default", 1, 16));
        panel.add(label, "North");
        final JPanel optionsPanel = new JPanel(new BorderLayout());
        final JPanel labelPanel = new JPanel(new GridLayout(0, 1));
        final JPanel fieldPanel = new JPanel(new GridLayout(0, 1));
        optionsPanel.add(labelPanel, "West");
        optionsPanel.add(fieldPanel, "Center");
        final JButton forceButton = new JButton("Force update!");
        forceButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                GameUpdater.forceUpdate = true;
                forceButton.setText("Will force!");
                forceButton.setEnabled(false);
            }
        });
        labelPanel.add(new JLabel("Force game update: ", 4));
        fieldPanel.add(forceButton);
        labelPanel.add(new JLabel("Game location on disk: ", 4));
        final TransparentLabel dirLink = new TransparentLabel(Util.getWorkingDirectory().toString()) {
            private static final long serialVersionUID = 0L;

            @Override
            public void paint(final Graphics g) {
                super.paint(g);
                int x = 0;
                int y = 0;
                final FontMetrics fm = g.getFontMetrics();
                final int width = fm.stringWidth(this.getText());
                final int height = fm.getHeight();
                if (this.getAlignmentX() == 2.0f) {
                    x = 0;
                } else if (this.getAlignmentX() == 0.0f) {
                    x = this.getBounds().width / 2 - width / 2;
                } else if (this.getAlignmentX() == 4.0f) {
                    x = this.getBounds().width - width;
                }
                y = this.getBounds().height / 2 + height / 2 - 1;
                g.drawLine(x + 2, y, x + width - 2, y);
            }

            @Override
            public void update(final Graphics g) {
                this.paint(g);
            }
        };
        dirLink.setCursor(Cursor.getPredefinedCursor(12));
        dirLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent arg0) {
                try {
                    Util.openLink(new URL("file://" + Util.getWorkingDirectory().getAbsolutePath()).toURI());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        dirLink.setForeground(new Color(2105599));
        fieldPanel.add(dirLink);
        panel.add(optionsPanel, "Center");
        final JPanel buttonsPanel = new JPanel(new BorderLayout());
        buttonsPanel.add(new JPanel(), "Center");
        final JButton doneButton = new JButton("Done");
        doneButton.addActionListener(ae -> OptionsPanel.this.setVisible(false));
        buttonsPanel.add(doneButton, "East");
        buttonsPanel.setBorder(new EmptyBorder(16, 0, 0, 0));
        panel.add(buttonsPanel, "South");
        this.add(panel);
        panel.setBorder(new EmptyBorder(16, 24, 24, 24));
        this.pack();
        this.setLocationRelativeTo(parent);
    }
}
