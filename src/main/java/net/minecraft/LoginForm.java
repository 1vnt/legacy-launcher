package net.minecraft;

import java.io.*;
import java.util.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.swing.event.*;
import java.net.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

public class LoginForm extends TransparentPanel {
    private static final int PANEL_SIZE = 100;
    private static final long serialVersionUID = 1L;
    private static final Color LINK_COLOR;
    public JTextField userName;
    public JPasswordField password;
    private TransparentCheckbox rememberBox;
    private TransparentButton launchButton;
    private TransparentButton optionsButton;
    private TransparentButton retryButton;
    private TransparentButton offlineButton;
    private TransparentLabel errorLabel;
    private LauncherFrame launcherFrame;
    private boolean outdated;
    private JScrollPane scrollPane;

    static {
        LINK_COLOR = new Color(8421631);
    }

    public LoginForm(final LauncherFrame launcherFrame) {
        this.userName = new JTextField(20);
        this.password = new JPasswordField(20);
        this.rememberBox = new TransparentCheckbox("Remember password");
        this.launchButton = new TransparentButton("Login");
        this.optionsButton = new TransparentButton("Options");
        this.retryButton = new TransparentButton("Try again");
        this.offlineButton = new TransparentButton("Play offline");
        this.errorLabel = new TransparentLabel("", 0);
        this.outdated = false;
        this.launcherFrame = launcherFrame;
        final BorderLayout gbl = new BorderLayout();
        this.setLayout(gbl);
        this.add(this.buildMainLoginPanel(), "Center");
        this.readUsername();
        final ActionListener al = arg0 -> LoginForm.this.doLogin();
        this.userName.addActionListener(al);
        this.password.addActionListener(al);
        this.retryButton.addActionListener(ae -> {
            LoginForm.this.errorLabel.setText("");
            LoginForm.this.removeAll();
            LoginForm.this.add(LoginForm.this.buildMainLoginPanel(), "Center");
            LoginForm.this.validate();
        });
        this.offlineButton.addActionListener(ae -> launcherFrame.playCached(LoginForm.this.userName.getText()));
        this.launchButton.addActionListener(al);
        this.optionsButton.addActionListener(ae -> new OptionsPanel(launcherFrame).setVisible(true));
    }

    public void doLogin() {
        this.setLoggingIn();
        new Thread() {
            @Override
            public void run() {
                try {
                    LoginForm.this.launcherFrame.login(LoginForm.this.userName.getText(), new String(LoginForm.this.password.getPassword()));
                } catch (Exception e) {
                    LoginForm.this.setError(e.toString());
                }
            }
        }.start();
    }

    private void readUsername() {
        try {
            final File lastLogin = new File(Util.getWorkingDirectory(), "lastlogin");
            final Cipher cipher = this.getCipher(2, "passwordfile");
            DataInputStream dis;
            if (lastLogin.exists()) {
                dis = new DataInputStream(new CipherInputStream(new FileInputStream(lastLogin), cipher));
                this.userName.setText(dis.readUTF());
                this.password.setText(dis.readUTF());
                this.rememberBox.setSelected(this.password.getPassword().length > 0);
                dis.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeUsername() {
        try {
            final File lastLogin = new File(Util.getWorkingDirectory(), "lastlogin");
            final Cipher cipher = this.getCipher(1, "passwordfile");
            DataOutputStream dos;
            dos = new DataOutputStream(new CipherOutputStream(new FileOutputStream(lastLogin), cipher));
            dos.writeUTF(this.userName.getText());
            dos.writeUTF(this.rememberBox.isSelected() ? new String(this.password.getPassword()) : "");
            dos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Cipher getCipher(final int mode, final String password) throws Exception {
        final Random random = new Random(43287234L);
        final byte[] salt = new byte[8];
        random.nextBytes(salt);
        final PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, 5);
        final SecretKey pbeKey = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(new PBEKeySpec(password.toCharArray()));
        final Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
        cipher.init(mode, pbeKey, pbeParamSpec);
        return cipher;
    }

    private JScrollPane getUpdateNews() {
        if (this.scrollPane != null) {
            return this.scrollPane;
        }
        try {
            final JTextPane editorPane = new JTextPane() {
                private static final long serialVersionUID = 1L;
            };
            editorPane.setText("<html><body><font color=\"#808080\"><br><br><br><br><br><br><br><center>Loading update news..</center></font></body></html>");
            editorPane.addHyperlinkListener(he -> {
                if (he.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Util.openLink(he.getURL().toURI());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            new Thread(() -> {
                try {
                    editorPane.setPage(new URL("http://mcupdate.tumblr.com/"));
                } catch (Exception e) {
                    e.printStackTrace();
                    editorPane.setText("<html><body><font color=\"#808080\"><br><br><br><br><br><br><br><center>Failed to update news<br>" + e.toString() + "</center></font></body></html>");
                }
            }).start();
            editorPane.setBackground(Color.DARK_GRAY);
            editorPane.setEditable(false);
            (this.scrollPane = new JScrollPane(editorPane)).setBorder(null);
            editorPane.setMargin(null);
            this.scrollPane.setBorder(new MatteBorder(0, 0, 2, 0, Color.BLACK));
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        return this.scrollPane;
    }

    private JPanel buildMainLoginPanel() {
        final JPanel p = new TransparentPanel(new BorderLayout());
        p.add(this.getUpdateNews(), "Center");
        final JPanel southPanel = new TexturedPanel();
        southPanel.setLayout(new BorderLayout());
        southPanel.add(new LogoPanel(), "West");
        southPanel.add(new TransparentPanel(), "Center");
        southPanel.add(this.center(this.buildLoginPanel()), "East");
        southPanel.setPreferredSize(new Dimension(100, 100));
        p.add(southPanel, "South");
        return p;
    }

    private JPanel buildLoginPanel() {
        final TransparentPanel panel = new TransparentPanel();
        panel.setInsets(4, 0, 4, 0);
        final BorderLayout layout = new BorderLayout();
        layout.setHgap(0);
        layout.setVgap(8);
        panel.setLayout(layout);
        final GridLayout gl1 = new GridLayout(0, 1);
        gl1.setVgap(2);
        final GridLayout gl2 = new GridLayout(0, 1);
        gl2.setVgap(2);
        final GridLayout gl3 = new GridLayout(0, 1);
        gl3.setVgap(2);
        final TransparentPanel titles = new TransparentPanel(gl1);
        final TransparentPanel values = new TransparentPanel(gl2);
        titles.add(new TransparentLabel("Username:", 4));
        titles.add(new TransparentLabel("Password:", 4));
        titles.add(new TransparentLabel("", 4));
        values.add(this.userName);
        values.add(this.password);
        values.add(this.rememberBox);
        panel.add(titles, "West");
        panel.add(values, "Center");
        final TransparentPanel loginPanel = new TransparentPanel(new BorderLayout());
        final TransparentPanel third = new TransparentPanel(gl3);
        titles.setInsets(0, 0, 0, 4);
        third.setInsets(0, 10, 0, 10);
        third.add(this.optionsButton);
        third.add(this.launchButton);
        try {
            if (this.outdated) {
                final TransparentLabel accountLink = this.getUpdateLink();
                third.add(accountLink);
            } else {
                final TransparentLabel accountLink = new TransparentLabel("Need account?") {
                    private static final long serialVersionUID = 0L;

                    @Override
                    public void paint(final Graphics g) {
                        super.paint(g);
                        int x = 0;
                        int y;
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
                accountLink.setCursor(Cursor.getPredefinedCursor(12));
                accountLink.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(final MouseEvent arg0) {
                        try {
                            Util.openLink(new URL("http://www.minecraft.net/register.jsp").toURI());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                accountLink.setForeground(LoginForm.LINK_COLOR);
                third.add(accountLink);
            }
        } catch (Error ignored) {
        }
        loginPanel.add(third, "Center");
        panel.add(loginPanel, "East");
        this.errorLabel.setFont(new Font(null, 2, 16));
        this.errorLabel.setForeground(new Color(16728128));
        this.errorLabel.setText("");
        panel.add(this.errorLabel, "North");
        return panel;
    }

    private TransparentLabel getUpdateLink() {
        final TransparentLabel accountLink = new TransparentLabel("You need to update the launcher!") {
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
        accountLink.setCursor(Cursor.getPredefinedCursor(12));
        accountLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent arg0) {
                try {
                    Util.openLink(new URL("http://www.minecraft.net/download.jsp").toURI());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        accountLink.setForeground(LoginForm.LINK_COLOR);
        return accountLink;
    }

    private JPanel buildMainOfflinePanel() {
        final JPanel p = new TransparentPanel(new BorderLayout());
        p.add(this.getUpdateNews(), "Center");
        final JPanel southPanel = new TexturedPanel();
        southPanel.setLayout(new BorderLayout());
        southPanel.add(new LogoPanel(), "West");
        southPanel.add(new TransparentPanel(), "Center");
        southPanel.add(this.center(this.buildOfflinePanel()), "East");
        southPanel.setPreferredSize(new Dimension(100, 100));
        p.add(southPanel, "South");
        return p;
    }

    private Component center(final Component c) {
        final TransparentPanel tp = new TransparentPanel(new GridBagLayout());
        tp.add(c);
        return tp;
    }

    private TransparentPanel buildOfflinePanel() {
        final TransparentPanel panel = new TransparentPanel();
        panel.setInsets(0, 0, 0, 20);
        final BorderLayout layout = new BorderLayout();
        panel.setLayout(layout);
        final TransparentPanel loginPanel = new TransparentPanel(new BorderLayout());
        final GridLayout gl = new GridLayout(0, 1);
        gl.setVgap(2);
        final TransparentPanel pp = new TransparentPanel(gl);
        pp.setInsets(0, 8, 0, 0);
        pp.add(this.retryButton);
        pp.add(this.offlineButton);
        loginPanel.add(pp, "East");
        final boolean canPlayOffline = this.launcherFrame.canPlayOffline(this.userName.getText());
        this.offlineButton.setEnabled(canPlayOffline);
        if (!canPlayOffline) {
            loginPanel.add(new TransparentLabel("(Not downloaded)", 4), "South");
        }
        panel.add(loginPanel, "Center");
        final TransparentPanel p2 = new TransparentPanel(new GridLayout(0, 1));
        this.errorLabel.setFont(new Font(null, 2, 16));
        this.errorLabel.setForeground(new Color(16728128));
        p2.add(this.errorLabel);
        if (this.outdated) {
            final TransparentLabel accountLink = this.getUpdateLink();
            p2.add(accountLink);
        }
        loginPanel.add(p2, "Center");
        return panel;
    }

    public void setError(final String errorMessage) {
        this.removeAll();
        this.add(this.buildMainLoginPanel(), "Center");
        this.errorLabel.setText(errorMessage);
        this.validate();
    }

    public void loginOk() {
        this.writeUsername();
    }

    public void setLoggingIn() {
        this.removeAll();
        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(this.getUpdateNews(), "Center");
        final JPanel southPanel = new TexturedPanel();
        southPanel.setLayout(new BorderLayout());
        southPanel.add(new LogoPanel(), "West");
        southPanel.add(new TransparentPanel(), "Center");
        final JLabel label = new TransparentLabel("Logging in...                      ", 0);
        label.setFont(new Font(null, 1, 16));
        southPanel.add(this.center(label), "East");
        southPanel.setPreferredSize(new Dimension(100, 100));
        panel.add(southPanel, "South");
        this.add(panel, "Center");
        this.validate();
    }

    public void setNoNetwork() {
        this.removeAll();
        this.add(this.buildMainOfflinePanel(), "Center");
        this.validate();
    }

    public void checkAutologin() {
        if (this.password.getPassword().length > 0) {
            this.launcherFrame.login(this.userName.getText(), new String(this.password.getPassword()));
        }
    }

    public void setOutdated() {
        this.outdated = true;
    }
}
