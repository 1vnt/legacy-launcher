package net.minecraft;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.security.PublicKey;
import java.security.cert.Certificate;

public class Util {
    private static File workDir;

    static {
        Util.workDir = null;
    }

    public static File getWorkingDirectory() {
        if (Util.workDir == null) {
            Util.workDir = getWorkingDirectory("leg-minecraft");
        }
        return Util.workDir;
    }

    public static File getWorkingDirectory(final String applicationName) {
        final String userHome = System.getProperty("user.home", ".");
        File workingDirectory;
        switch (getPlatform()) {
            case linux:
                workingDirectory = new File(userHome, '.' + applicationName + '/');
                break;
            case solaris: {
                workingDirectory = new File(userHome, '.' + applicationName + '/');
                break;
            }
            case windows: {
                final String applicationData = System.getenv("APPDATA");
                if (applicationData != null) {
                    workingDirectory = new File(applicationData, "." + applicationName + '/');
                    break;
                }
                workingDirectory = new File(userHome, '.' + applicationName + '/');
                break;
            }
            case macos: {
                workingDirectory = new File(userHome, "Library/Application Support/" + applicationName);
                break;
            }
            default: {
                workingDirectory = new File(userHome, String.valueOf(applicationName) + '/');
                break;
            }
        }
        if (!workingDirectory.exists() && !workingDirectory.mkdirs()) {
            throw new RuntimeException("The working directory could not be created: " + workingDirectory);
        }
        return workingDirectory;
    }

    private static OS getPlatform() {
        final String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return OS.windows;
        }
        if (osName.contains("mac")) {
            return OS.macos;
        }
        if (osName.contains("solaris")) {
            return OS.solaris;
        }
        if (osName.contains("sunos")) {
            return OS.solaris;
        }
        if (osName.contains("linux")) {
            return OS.linux;
        }
        if (osName.contains("unix")) {
            return OS.linux;
        }
        return OS.unknown;
    }

    public static String excutePost(final String targetURL, final String urlParameters) {
        HttpsURLConnection connection = null;
        try {
            final URL url = new URL(targetURL);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", new StringBuilder().append(urlParameters.getBytes().length).toString());
            connection.setRequestProperty("Content-Language", "en-US");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.connect();
            final Certificate[] certs = connection.getServerCertificates();
            final byte[] bytes = new byte[294];
            final DataInputStream dis = new DataInputStream(Util.class.getResourceAsStream("/minecraft.key"));
            dis.readFully(bytes);
            dis.close();
            final Certificate c = certs[0];
            final PublicKey pk = c.getPublicKey();
            final byte[] data = pk.getEncoded();
            for (int i = 0; i < data.length; ++i) {
                if (data[i] != bytes[i]) {
                    throw new RuntimeException("Public key mismatch");
                }
            }
            final DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            final InputStream is = connection.getInputStream();
            final BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            final StringBuffer response = new StringBuffer();
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static boolean isEmpty(final String str) {
        return str == null || str.length() == 0;
    }

    public static void openLink(final URI uri) {
        try {
            final Object o = Class.forName("java.awt.Desktop").getMethod("getDesktop", new Class[0]).invoke(null);
            o.getClass().getMethod("browse", URI.class).invoke(o, uri);
        } catch (Throwable e) {
            System.out.println("Failed to open link " + uri.toString());
        }
    }

    private enum OS {
        linux("linux", 0),
        solaris("solaris", 1),
        windows("windows", 2),
        macos("macos", 3),
        unknown("unknown", 4);

        OS(final String s, final int n) {
        }
    }
}