package net.minecraft;

import java.applet.*;
import java.util.*;
import javax.imageio.*;
import java.io.*;
import java.awt.image.*;
import java.awt.*;
import java.net.*;
import java.awt.event.*;

public class Launcher extends Applet implements Runnable, AppletStub, MouseListener {
    private static final long serialVersionUID = 1L;
    public Map<String, String> customParameters;
    private GameUpdater gameUpdater;
    private boolean gameUpdaterStarted;
    private Applet applet;
    private Image bgImage;
    private boolean active;
    private int context;
    private boolean hasMouseListener;
    private VolatileImage img;

    public Launcher() {
        this.customParameters = new HashMap<String, String>();
        this.gameUpdaterStarted = false;
        this.active = false;
        this.context = 0;
        this.hasMouseListener = false;
    }

    @Override
    public boolean isActive() {
        if (this.context == 0) {
            this.context = -1;
            try {
                if (this.getAppletContext() != null) {
                    this.context = 1;
                }
            } catch (Exception ex) {
            }
        }
        if (this.context == -1) {
            return this.active;
        }
        return super.isActive();
    }

    public void init(final String userName, final String latestVersion, final String downloadTicket, final String sessionId) {
        try {
            this.bgImage = ImageIO.read(LoginForm.class.getResource("/dirt.png")).getScaledInstance(32, 32, 16);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.customParameters.put("username", userName);
        this.customParameters.put("sessionid", sessionId);
        this.gameUpdater = new GameUpdater(latestVersion, "minecraft.jar?user=" + userName + "&ticket=" + downloadTicket, this.customParameters.containsKey("noupdate"));
    }

    public boolean canPlayOffline() {
        return this.gameUpdater.canPlayOffline();
    }

    @Override
    public void init() {
        if (this.applet != null) {
            this.applet.init();
            return;
        }
        this.init(this.getParameter("userName"), this.getParameter("latestVersion"), this.getParameter("downloadTicket"), this.getParameter("sessionId"));
    }

    @Override
    public void start() {
        if (this.applet != null) {
            this.applet.start();
            return;
        }
        if (this.gameUpdaterStarted) {
            return;
        }
        Thread t = new Thread() {
            @Override
            public void run() {
                Launcher.this.gameUpdater.run();
                try {
                    if (!Launcher.this.gameUpdater.fatalError) {
                        Launcher.this.replace(Launcher.this.gameUpdater.createApplet());
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (InstantiationException e2) {
                    e2.printStackTrace();
                } catch (IllegalAccessException e3) {
                    e3.printStackTrace();
                }
            }
        };
        t.setDaemon(true);
        t.start();
        t = new Thread() {
            @Override
            public void run() {
                while (Launcher.this.applet == null) {
                    Launcher.this.repaint();
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        t.setDaemon(true);
        t.start();
        this.gameUpdaterStarted = true;
    }

    @Override
    public void stop() {
        if (this.applet != null) {
            this.active = false;
            this.applet.stop();
        }
    }

    @Override
    public void destroy() {
        if (this.applet != null) {
            this.applet.destroy();
        }
    }

    public void replace(final Applet applet) {
        (this.applet = applet).setStub(this);
        applet.setSize(this.getWidth(), this.getHeight());
        this.setLayout(new BorderLayout());
        this.add(applet, "Center");
        applet.init();
        this.active = true;
        applet.start();
        this.validate();
    }

    @Override
    public void update(final Graphics g) {
        this.paint(g);
    }

    @Override
    public void paint(final Graphics g2) {
        if (this.applet != null) {
            return;
        }
        final int w = this.getWidth() / 2;
        final int h = this.getHeight() / 2;
        if (this.img == null || this.img.getWidth() != w || this.img.getHeight() != h) {
            this.img = this.createVolatileImage(w, h);
        }
        final Graphics g3 = this.img.getGraphics();
        for (int x = 0; x <= w / 32; ++x) {
            for (int y = 0; y <= h / 32; ++y) {
                g3.drawImage(this.bgImage, x * 32, y * 32, null);
            }
        }
        if (this.gameUpdater.pauseAskUpdate) {
            if (!this.hasMouseListener) {
                this.hasMouseListener = true;
                this.addMouseListener(this);
            }
            g3.setColor(Color.LIGHT_GRAY);
            String msg = "New update available";
            g3.setFont(new Font(null, 1, 20));
            FontMetrics fm = g3.getFontMetrics();
            g3.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 - fm.getHeight() * 2);
            g3.setFont(new Font(null, 0, 12));
            fm = g3.getFontMetrics();
            g3.fill3DRect(w / 2 - 56 - 8, h / 2, 56, 20, true);
            g3.fill3DRect(w / 2 + 8, h / 2, 56, 20, true);
            msg = "Would you like to update?";
            g3.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 - 8);
            g3.setColor(Color.BLACK);
            msg = "Yes";
            g3.drawString(msg, w / 2 - 56 - 8 - fm.stringWidth(msg) / 2 + 28, h / 2 + 14);
            msg = "Not now";
            g3.drawString(msg, w / 2 + 8 - fm.stringWidth(msg) / 2 + 28, h / 2 + 14);
        } else {
            g3.setColor(Color.LIGHT_GRAY);
            String msg = "Updating Minecraft";
            if (this.gameUpdater.fatalError) {
                msg = "Failed to launch";
            }
            g3.setFont(new Font(null, 1, 20));
            FontMetrics fm = g3.getFontMetrics();
            g3.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 - fm.getHeight() * 2);
            g3.setFont(new Font(null, 0, 12));
            fm = g3.getFontMetrics();
            msg = this.gameUpdater.getDescriptionForState();
            if (this.gameUpdater.fatalError) {
                msg = this.gameUpdater.fatalErrorDescription;
            }
            g3.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 + fm.getHeight() * 1);
            msg = this.gameUpdater.subtaskMessage;
            g3.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 + fm.getHeight() * 2);
            if (!this.gameUpdater.fatalError) {
                g3.setColor(Color.black);
                g3.fillRect(64, h - 64, w - 128 + 1, 5);
                g3.setColor(new Color(32768));
                g3.fillRect(64, h - 64, this.gameUpdater.percentage * (w - 128) / 100, 4);
                g3.setColor(new Color(2138144));
                g3.fillRect(65, h - 64 + 1, this.gameUpdater.percentage * (w - 128) / 100 - 2, 1);
            }
        }
        g3.dispose();
        g2.drawImage(this.img, 0, 0, w * 2, h * 2, null);
    }

    public void run() {
    }

    @Override
    public String getParameter(final String name) {
        final String custom = this.customParameters.get(name);
        if (custom != null) {
            return custom;
        }
        try {
            return super.getParameter(name);
        } catch (Exception e) {
            this.customParameters.put(name, null);
            return null;
        }
    }

    public void appletResize(final int width, final int height) {
    }

    @Override
    public URL getDocumentBase() {
        try {
            return new URL("http://www.minecraft.net/game/");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void mouseClicked(final MouseEvent arg0) {
    }

    public void mouseEntered(final MouseEvent arg0) {
    }

    public void mouseExited(final MouseEvent arg0) {
    }

    public void mousePressed(final MouseEvent me) {
        final int x = me.getX() / 2;
        final int y = me.getY() / 2;
        final int w = this.getWidth() / 2;
        final int h = this.getHeight() / 2;
        if (this.contains(x, y, w / 2 - 56 - 8, h / 2, 56, 20)) {
            this.removeMouseListener(this);
            this.gameUpdater.shouldUpdate = true;
            this.gameUpdater.pauseAskUpdate = false;
            this.hasMouseListener = false;
        }
        if (this.contains(x, y, w / 2 + 8, h / 2, 56, 20)) {
            this.removeMouseListener(this);
            this.gameUpdater.shouldUpdate = false;
            this.gameUpdater.pauseAskUpdate = false;
            this.hasMouseListener = false;
        }
    }

    private boolean contains(final int x, final int y, final int xx, final int yy, final int w, final int h) {
        return x >= xx && y >= yy && x < xx + w && y < yy + h;
    }

    public void mouseReleased(final MouseEvent arg0) {
    }
}
