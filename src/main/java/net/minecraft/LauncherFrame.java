package net.minecraft;

import java.util.*;
import javax.imageio.*;
import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.net.*;
import javax.swing.*;

public class LauncherFrame extends Frame
{
    public static final int VERSION = 13;
    private static final long serialVersionUID = 1L;
    public Map<String, String> customParameters;
    public Launcher launcher;
    public LoginForm loginForm;
    
    public LauncherFrame() {
        super("Minecraft Launcher");
        this.customParameters = new HashMap<String, String>();
        this.setBackground(Color.BLACK);
        this.loginForm = new LoginForm(this);
        final JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.add(this.loginForm, "Center");
        p.setPreferredSize(new Dimension(854, 480));
        this.setLayout(new BorderLayout());
        this.add(p, "Center");
        this.pack();
        this.setLocationRelativeTo(null);
        try {
            this.setIconImage(ImageIO.read(LauncherFrame.class.getResource("/favicon.png")));
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent arg0) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(30000L);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("FORCING EXIT!");
                        System.exit(0);
                    }
                }.start();
                if (LauncherFrame.this.launcher != null) {
                    LauncherFrame.this.launcher.stop();
                    LauncherFrame.this.launcher.destroy();
                }
                System.exit(0);
            }
        });
    }
    
    public void playCached(String userName) {
        try {
            if (userName == null || userName.length() <= 0) {
                userName = "Player";
            }
            this.launcher = new Launcher();
            this.launcher.customParameters.putAll(this.customParameters);
            this.launcher.customParameters.put("userName", userName);
            this.launcher.init();
            this.removeAll();
            this.add(this.launcher, "Center");
            this.validate();
            this.launcher.start();
            this.loginForm = null;
            this.setTitle("Minecraft");
        }
        catch (Exception e) {
            e.printStackTrace();
            this.showError(e.toString());
        }
    }

    /**
     * Login to minecraft account
     *
     * @param userName username to login with
     * @param password password to login with
     */
    public void login(final String userName, final String password) {
        try {
            final String parameters = "user=" + URLEncoder.encode(userName, "UTF-8") + "&password=" + URLEncoder.encode(password, "UTF-8") + "&version=" + 13;
            final String result = Util.excutePost("https://login.minecraft.net/", parameters);
            if (result == null) {
                this.showError("Can't connect to minecraft.net");
                this.loginForm.setNoNetwork();
                return;
            }
            if (!result.contains(":")) {
                if (result.trim().equals("Bad login")) {
                    this.showError("Login failed");
                }
                else if (result.trim().equals("Old version")) {
                    this.loginForm.setOutdated();
                    this.showError("Outdated launcher");
                }
                else {
                    this.showError(result);
                }
                this.loginForm.setNoNetwork();
                return;
            }
            final String[] values = result.split(":");
            this.launcher = new Launcher();
            this.launcher.customParameters.putAll(this.customParameters);
            this.launcher.customParameters.put("userName", values[2].trim());
            this.launcher.customParameters.put("latestVersion", values[0].trim());
            this.launcher.customParameters.put("downloadTicket", values[1].trim());
            this.launcher.customParameters.put("sessionId", values[3].trim());
            this.launcher.init();
            this.removeAll();
            this.add(this.launcher, "Center");
            this.validate();
            this.launcher.start();
            this.loginForm.loginOk();
            this.loginForm = null;
            this.setTitle("Minecraft");
        }
        catch (Exception e) {
            e.printStackTrace();
            this.showError(e.toString());
            this.loginForm.setNoNetwork();
        }
    }
    
    private void showError(final String error) {
        this.removeAll();
        this.add(this.loginForm);
        this.loginForm.setError(error);
        this.validate();
    }
    
    public boolean canPlayOffline(final String userName) {
        final Launcher launcher = new Launcher();
        launcher.customParameters.putAll(this.customParameters);
        launcher.init(userName, null, null, null);
        return launcher.canPlayOffline();
    }
    
    public static void main(final String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception ex) {}
        final LauncherFrame launcherFrame = new LauncherFrame();
        launcherFrame.setVisible(true);
        launcherFrame.customParameters.put("stand-alone", "true");
        String userName = null;
        String password = null;
        for (final String argument : args) {
            if (argument.startsWith("-u=") || argument.startsWith("--user=")) {
                userName = getArgValue(argument);
                launcherFrame.customParameters.put("username", userName);
                launcherFrame.loginForm.userName.setText(userName);
            }
            else if (argument.startsWith("-p=") || argument.startsWith("--password=")) {
                password = getArgValue(argument);
                launcherFrame.customParameters.put("password", password);
                launcherFrame.loginForm.password.setText(password);
            }
            else if (argument.startsWith("--noupdate")) {
                launcherFrame.customParameters.put("noupdate", "true");
            }
        }
        if (args.length >= 3) {
            String ip = args[2];
            String port = "25565";
            if (ip.contains(":")) {
                final String[] parts = ip.split(":");
                ip = parts[0];
                port = parts[1];
            }
            launcherFrame.customParameters.put("server", ip);
            launcherFrame.customParameters.put("port", port);
        }
    }
    
    private static String getArgValue(final String argument) {
        final int index = argument.indexOf(61);
        if (index < 0) {
            return "";
        }
        return argument.substring(index + 1);
    }
}
