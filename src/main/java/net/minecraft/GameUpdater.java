package net.minecraft;

import java.applet.*;
import java.security.*;
import java.math.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.cert.Certificate;
import java.util.jar.*;
import java.security.cert.*;
import java.util.*;

public class GameUpdater implements Runnable
{
    public static final int STATE_INIT = 1;
    public static final int STATE_DETERMINING_PACKAGES = 2;
    public static final int STATE_CHECKING_CACHE = 3;
    public static final int STATE_DOWNLOADING = 4;
    public static final int STATE_EXTRACTING_PACKAGES = 5;
    public static final int STATE_UPDATING_CLASSPATH = 6;
    public static final int STATE_SWITCHING_APPLET = 7;
    public static final int STATE_INITIALIZE_REAL_APPLET = 8;
    public static final int STATE_START_REAL_APPLET = 9;
    public static final int STATE_DONE = 10;
    public int percentage;
    public int currentSizeDownload;
    public int totalSizeDownload;
    public int currentSizeExtract;
    public int totalSizeExtract;
    protected URL[] urlList;
    private static ClassLoader classLoader;
    protected Thread loaderThread;
    protected Thread animationThread;
    public boolean fatalError;
    public String fatalErrorDescription;
    protected String subtaskMessage;
    protected int state;
    protected boolean lzmaSupported;
    protected boolean pack200Supported;
    protected String[] genericErrorMessage;
    protected boolean certificateRefused;
    protected String[] certificateRefusedMessage;
    protected static boolean natives_loaded;
    public static boolean forceUpdate;
    private String latestVersion;
    private String mainGameUrl;
    public boolean pauseAskUpdate;
    public boolean shouldUpdate;
    public boolean skipUpdate;
    
    static {
        GameUpdater.natives_loaded = false;
        GameUpdater.forceUpdate = false;
    }
    
    public GameUpdater(final String latestVersion, final String mainGameUrl, final boolean skipUpdate) {
        this.subtaskMessage = "";
        this.state = 1;
        this.lzmaSupported = false;
        this.pack200Supported = false;
        this.genericErrorMessage = new String[] { "An error occured while loading the applet.", "Please contact support to resolve this issue.", "<placeholder for error message>" };
        this.certificateRefusedMessage = new String[] { "Permissions for Applet Refused.", "Please accept the permissions dialog to allow", "the applet to continue the loading process." };
        this.latestVersion = latestVersion;
        this.mainGameUrl = mainGameUrl;
        this.skipUpdate = skipUpdate;
    }
    
    public void init() {
        this.state = 1;
        try {
            Class.forName("LZMA.LzmaInputStream");
            this.lzmaSupported = true;
        }
        catch (Throwable t) {}
        try {
            Pack200.class.getSimpleName();
            this.pack200Supported = true;
        }
        catch (Throwable t2) {}
    }
    
    private String generateStacktrace(final Exception exception) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        exception.printStackTrace(printWriter);
        return result.toString();
    }
    
    protected String getDescriptionForState() {
        switch (this.state) {
            case 1: {
                return "Initializing loader";
            }
            case 2: {
                return "Determining packages to load";
            }
            case 3: {
                return "Checking cache for existing files";
            }
            case 4: {
                return "Downloading packages";
            }
            case 5: {
                return "Extracting downloaded packages";
            }
            case 6: {
                return "Updating classpath";
            }
            case 7: {
                return "Switching applet";
            }
            case 8: {
                return "Initializing real applet";
            }
            case 9: {
                return "Starting real applet";
            }
            case 10: {
                return "Done loading";
            }
            default: {
                return "unknown state";
            }
        }
    }
    
    protected String trimExtensionByCapabilities(String file) {
        if (!this.pack200Supported) {
            file = file.replaceAll(".pack", "");
        }
        if (!this.lzmaSupported) {
            file = file.replaceAll(".lzma", "");
        }
        return file;
    }
    
    protected void loadJarURLs() throws Exception {
        this.state = 2;
        String jarList = "lwjgl.jar, jinput.jar, lwjgl_util.jar, " + this.mainGameUrl;
        jarList = this.trimExtensionByCapabilities(jarList);
        final StringTokenizer jar = new StringTokenizer(jarList, ", ");
        final int jarCount = jar.countTokens() + 1;
        this.urlList = new URL[jarCount];
        final URL path = new URL("http://s3.amazonaws.com/MinecraftDownload/");
        for (int i = 0; i < jarCount - 1; ++i) {
            this.urlList[i] = new URL(path, jar.nextToken());
        }
        final String osName = System.getProperty("os.name");
        String nativeJar = null;
        if (osName.startsWith("Win")) {
            nativeJar = "windows_natives.jar.lzma";
        }
        else if (osName.startsWith("Linux")) {
            nativeJar = "linux_natives.jar.lzma";
        }
        else if (osName.startsWith("Mac")) {
            nativeJar = "macosx_natives.jar.lzma";
        }
        else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
            nativeJar = "solaris_natives.jar.lzma";
        }
        else {
            this.fatalErrorOccured("OS (" + osName + ") not supported", null);
        }
        if (nativeJar == null) {
            this.fatalErrorOccured("no lwjgl natives files found", null);
        }
        else {
            nativeJar = this.trimExtensionByCapabilities(nativeJar);
            this.urlList[jarCount - 1] = new URL(path, nativeJar);
        }
    }
    
    public void run() {
        this.init();
        this.state = 3;
        this.percentage = 5;
        try {
            this.loadJarURLs();
            final String path = AccessController.doPrivileged((PrivilegedExceptionAction<String>)new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    return Util.getWorkingDirectory() + File.separator + "bin" + File.separator;
                }
            });
            final File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            if (this.latestVersion != null) {
                final File versionFile = new File(dir, "version");
                boolean cacheAvailable = false;
                if (!this.skipUpdate && !GameUpdater.forceUpdate && versionFile.exists() && (this.latestVersion.equals("-1") || this.latestVersion.equals(this.readVersionFile(versionFile)))) {
                    cacheAvailable = true;
                    this.percentage = 90;
                }
                if (!this.skipUpdate && (GameUpdater.forceUpdate || !cacheAvailable)) {
                    this.shouldUpdate = true;
                    if (!GameUpdater.forceUpdate && versionFile.exists()) {
                        this.checkShouldUpdate();
                    }
                    if (this.shouldUpdate) {
                        this.writeVersionFile(versionFile, "");
                        this.downloadJars(path);
                        this.extractJars(path);
                        this.extractNatives(path);
                        if (this.latestVersion != null) {
                            this.percentage = 90;
                            this.writeVersionFile(versionFile, this.latestVersion);
                        }
                    }
                    else {
                        cacheAvailable = true;
                        this.percentage = 90;
                    }
                }
            }
            this.updateClassPath(dir);
            this.state = 10;
        }
        catch (AccessControlException ace) {
            this.fatalErrorOccured(ace.getMessage(), ace);
            this.certificateRefused = true;
        }
        catch (Exception e) {
            this.fatalErrorOccured(e.getMessage(), e);
        }
        finally {
            this.loaderThread = null;
        }
        this.loaderThread = null;
    }
    
    private void checkShouldUpdate() {
        this.pauseAskUpdate = true;
        while (this.pauseAskUpdate) {
            try {
                Thread.sleep(1000L);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    protected String readVersionFile(final File file) throws Exception {
        final DataInputStream dis = new DataInputStream(new FileInputStream(file));
        final String version = dis.readUTF();
        dis.close();
        return version;
    }
    
    protected void writeVersionFile(final File file, final String version) throws Exception {
        final DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
        dos.writeUTF(version);
        dos.close();
    }
    
    protected void updateClassPath(final File dir) throws Exception {
        this.state = 6;
        this.percentage = 95;
        final URL[] urls = new URL[this.urlList.length];
        for (int i = 0; i < this.urlList.length; ++i) {
            urls[i] = new File(dir, this.getJarName(this.urlList[i])).toURI().toURL();
        }
        if (GameUpdater.classLoader == null) {
            GameUpdater.classLoader = new URLClassLoader(urls) {
                @Override
                protected PermissionCollection getPermissions(final CodeSource codesource) {
                    PermissionCollection perms = null;
                    try {
                        final Method method = SecureClassLoader.class.getDeclaredMethod("getPermissions", CodeSource.class);
                        method.setAccessible(true);
                        perms = (PermissionCollection)method.invoke(this.getClass().getClassLoader(), codesource);
                        final String host = "www.minecraft.net";
                        if (host != null && host.length() > 0) {
                            perms.add(new SocketPermission(host, "connect,accept"));
                        }
                        else {
                            codesource.getLocation().getProtocol().equals("file");
                        }
                        perms.add(new FilePermission("<<ALL FILES>>", "read"));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    return perms;
                }
            };
        }
        String path = dir.getAbsolutePath();
        if (!path.endsWith(File.separator)) {
            path = String.valueOf(path) + File.separator;
        }
        this.unloadNatives(path);
        System.setProperty("org.lwjgl.librarypath", String.valueOf(path) + "natives");
        System.setProperty("net.java.games.input.librarypath", String.valueOf(path) + "natives");
        GameUpdater.natives_loaded = true;
    }
    
    private void unloadNatives(final String nativePath) {
        if (!GameUpdater.natives_loaded) {
            return;
        }
        try {
            final Field field = ClassLoader.class.getDeclaredField("loadedLibraryNames");
            field.setAccessible(true);
            final Vector<String> libs = (Vector<String>)field.get(this.getClass().getClassLoader());
            final String path = new File(nativePath).getCanonicalPath();
            for (int i = 0; i < libs.size(); ++i) {
                final String s = libs.get(i);
                if (s.startsWith(path)) {
                    libs.remove(i);
                    --i;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public Applet createApplet() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        final Class<Applet> appletClass = (Class<Applet>)GameUpdater.classLoader.loadClass("net.minecraft.client.MinecraftApplet");
        return appletClass.newInstance();
    }
    
    protected void downloadJars(final String path) throws Exception {
        final File versionFile = new File(path, "md5s");
        final Properties md5s = new Properties();
        if (versionFile.exists()) {
            try {
                final FileInputStream fis = new FileInputStream(versionFile);
                md5s.load(fis);
                fis.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.state = 4;
        final int[] fileSizes = new int[this.urlList.length];
        final boolean[] skip = new boolean[this.urlList.length];
        for (int i = 0; i < this.urlList.length; ++i) {
            final URLConnection urlconnection = this.urlList[i].openConnection();
            urlconnection.setDefaultUseCaches(false);
            skip[i] = false;
            if (urlconnection instanceof HttpURLConnection) {
                ((HttpURLConnection)urlconnection).setRequestMethod("HEAD");
                final String etagOnDisk = "\"" + md5s.getProperty(this.getFileName(this.urlList[i])) + "\"";
                if (!GameUpdater.forceUpdate && etagOnDisk != null) {
                    urlconnection.setRequestProperty("If-None-Match", etagOnDisk);
                }
                final int code = ((HttpURLConnection)urlconnection).getResponseCode();
                if (code / 100 == 3) {
                    skip[i] = true;
                }
            }
            fileSizes[i] = urlconnection.getContentLength();
            this.totalSizeDownload += fileSizes[i];
        }
        final int percentage = 10;
        this.percentage = percentage;
        final int initialPercentage = percentage;
        final byte[] buffer = new byte[65536];
        for (int j = 0; j < this.urlList.length; ++j) {
            if (skip[j]) {
                this.percentage = initialPercentage + fileSizes[j] * 45 / this.totalSizeDownload;
            }
            else {
                try {
                    md5s.remove(this.getFileName(this.urlList[j]));
                    md5s.store(new FileOutputStream(versionFile), "md5 hashes for downloaded files");
                }
                catch (Exception e2) {
                    e2.printStackTrace();
                }
                int unsuccessfulAttempts = 0;
                final int maxUnsuccessfulAttempts = 3;
                boolean downloadFile = true;
                while (downloadFile) {
                    downloadFile = false;
                    final URLConnection urlconnection = this.urlList[j].openConnection();
                    String etag = "";
                    if (urlconnection instanceof HttpURLConnection) {
                        urlconnection.setRequestProperty("Cache-Control", "no-cache");
                        urlconnection.connect();
                        etag = urlconnection.getHeaderField("ETag");
                        etag = etag.substring(1, etag.length() - 1);
                    }
                    final String currentFile = this.getFileName(this.urlList[j]);
                    final InputStream inputstream = this.getJarInputStream(currentFile, urlconnection);
                    final FileOutputStream fos = new FileOutputStream(String.valueOf(path) + currentFile);
                    long downloadStartTime = System.currentTimeMillis();
                    int downloadedAmount = 0;
                    int fileSize = 0;
                    String downloadSpeedMessage = "";
                    final MessageDigest m = MessageDigest.getInstance("MD5");
                    int bufferSize;
                    while ((bufferSize = inputstream.read(buffer, 0, buffer.length)) != -1) {
                        fos.write(buffer, 0, bufferSize);
                        m.update(buffer, 0, bufferSize);
                        this.currentSizeDownload += bufferSize;
                        fileSize += bufferSize;
                        this.percentage = initialPercentage + this.currentSizeDownload * 45 / this.totalSizeDownload;
                        this.subtaskMessage = "Retrieving: " + currentFile + " " + this.currentSizeDownload * 100 / this.totalSizeDownload + "%";
                        downloadedAmount += bufferSize;
                        final long timeLapse = System.currentTimeMillis() - downloadStartTime;
                        if (timeLapse >= 1000L) {
                            float downloadSpeed = downloadedAmount / (float)timeLapse;
                            downloadSpeed = (int)(downloadSpeed * 100.0f) / 100.0f;
                            downloadSpeedMessage = " @ " + downloadSpeed + " KB/sec";
                            downloadedAmount = 0;
                            downloadStartTime += 1000L;
                        }
                        this.subtaskMessage = String.valueOf(this.subtaskMessage) + downloadSpeedMessage;
                    }
                    inputstream.close();
                    fos.close();
                    String md5;
                    for (md5 = new BigInteger(1, m.digest()).toString(16); md5.length() < 32; md5 = "0" + md5) {}
                    boolean md5Matches = true;
                    if (etag != null) {
                        md5Matches = md5.equals(etag);
                    }
                    if (urlconnection instanceof HttpURLConnection) {
                        Label_0895: {
                            if (md5Matches) {
                                if (fileSize != fileSizes[j]) {
                                    if (fileSizes[j] > 0) {
                                        break Label_0895;
                                    }
                                }
                                try {
                                    md5s.setProperty(this.getFileName(this.urlList[j]), etag);
                                    md5s.store(new FileOutputStream(versionFile), "md5 hashes for downloaded files");
                                }
                                catch (Exception e3) {
                                    e3.printStackTrace();
                                }
                                continue;
                            }
                        }
                        if (++unsuccessfulAttempts >= maxUnsuccessfulAttempts) {
                            throw new Exception("failed to download " + currentFile);
                        }
                        downloadFile = true;
                        this.currentSizeDownload -= fileSize;
                    }
                }
            }
        }
        this.subtaskMessage = "";
    }
    
    protected InputStream getJarInputStream(final String currentFile, final URLConnection urlconnection) throws Exception {
        final InputStream[] is = { null };
        for (int j = 0; j < 3 && is[0] == null; ++j) {
            final Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        is[0] = urlconnection.getInputStream();
                    }
                    catch (IOException ex) {}
                }
            };
            t.setName("JarInputStreamThread");
            t.start();
            int iterationCount = 0;
            while (is[0] == null && iterationCount++ < 5) {
                try {
                    t.join(1000L);
                }
                catch (InterruptedException ex) {}
            }
            if (is[0] == null) {
                try {
                    t.interrupt();
                    t.join();
                }
                catch (InterruptedException ex2) {}
            }
        }
        if (is[0] != null) {
            return is[0];
        }
        if (currentFile.equals("minecraft.jar")) {
            throw new Exception("Unable to download " + currentFile);
        }
        throw new Exception("Unable to download " + currentFile);
    }
    
    protected void extractLZMA(final String in, final String out) throws Exception {
        final File f = new File(in);
        if (!f.exists()) {
            return;
        }
        final FileInputStream fileInputHandle = new FileInputStream(f);
        final Class<?> clazz = Class.forName("LZMA.LzmaInputStream");
        final Constructor<?> constructor = clazz.getDeclaredConstructor(InputStream.class);
        InputStream inputHandle = (InputStream)constructor.newInstance(fileInputHandle);
        OutputStream outputHandle = new FileOutputStream(out);
        final byte[] buffer = new byte[16384];
        for (int ret = inputHandle.read(buffer); ret >= 1; ret = inputHandle.read(buffer)) {
            outputHandle.write(buffer, 0, ret);
        }
        inputHandle.close();
        outputHandle.close();
        outputHandle = null;
        inputHandle = null;
        f.delete();
    }
    
    protected void extractPack(final String in, final String out) throws Exception {
        final File f = new File(in);
        if (!f.exists()) {
            return;
        }
        final FileOutputStream fostream = new FileOutputStream(out);
        final JarOutputStream jostream = new JarOutputStream(fostream);
        final Pack200.Unpacker unpacker = Pack200.newUnpacker();
        unpacker.unpack(f, jostream);
        jostream.close();
        f.delete();
    }
    
    protected void extractJars(final String path) throws Exception {
        this.state = 5;
        final float increment = 10.0f / this.urlList.length;
        for (int i = 0; i < this.urlList.length; ++i) {
            this.percentage = 55 + (int)(increment * (i + 1));
            final String filename = this.getFileName(this.urlList[i]);
            if (filename.endsWith(".pack.lzma")) {
                this.subtaskMessage = "Extracting: " + filename + " to " + filename.replaceAll(".lzma", "");
                this.extractLZMA(String.valueOf(path) + filename, String.valueOf(path) + filename.replaceAll(".lzma", ""));
                this.subtaskMessage = "Extracting: " + filename.replaceAll(".lzma", "") + " to " + filename.replaceAll(".pack.lzma", "");
                this.extractPack(String.valueOf(path) + filename.replaceAll(".lzma", ""), String.valueOf(path) + filename.replaceAll(".pack.lzma", ""));
            }
            else if (filename.endsWith(".pack")) {
                this.subtaskMessage = "Extracting: " + filename + " to " + filename.replace(".pack", "");
                this.extractPack(String.valueOf(path) + filename, String.valueOf(path) + filename.replace(".pack", ""));
            }
            else if (filename.endsWith(".lzma")) {
                this.subtaskMessage = "Extracting: " + filename + " to " + filename.replace(".lzma", "");
                this.extractLZMA(String.valueOf(path) + filename, String.valueOf(path) + filename.replace(".lzma", ""));
            }
        }
    }
    
    protected void extractNatives(final String path) throws Exception {
        this.state = 5;
        final int initialPercentage = this.percentage;
        final String nativeJar = this.getJarName(this.urlList[this.urlList.length - 1]);
        Certificate[] certificate = Launcher.class.getProtectionDomain().getCodeSource().getCertificates();
        if (certificate == null) {
            final URL location = Launcher.class.getProtectionDomain().getCodeSource().getLocation();
            final JarURLConnection jurl = (JarURLConnection)new URL("jar:" + location.toString() + "!/net/minecraft/Launcher.class").openConnection();
            jurl.setDefaultUseCaches(true);
            try {
                certificate = jurl.getCertificates();
            }
            catch (Exception ex) {}
        }
        final File nativeFolder = new File(String.valueOf(path) + "natives");
        if (!nativeFolder.exists()) {
            nativeFolder.mkdir();
        }
        final File file = new File(String.valueOf(path) + nativeJar);
        if (!file.exists()) {
            return;
        }
        final JarFile jarFile = new JarFile(file, true);
        Enumeration<JarEntry> entities = jarFile.entries();
        this.totalSizeExtract = 0;
        while (entities.hasMoreElements()) {
            final JarEntry entry = entities.nextElement();
            if (!entry.isDirectory()) {
                if (entry.getName().indexOf(47) != -1) {
                    continue;
                }
                this.totalSizeExtract += (int)entry.getSize();
            }
        }
        this.currentSizeExtract = 0;
        entities = jarFile.entries();
        while (entities.hasMoreElements()) {
            final JarEntry entry = entities.nextElement();
            if (!entry.isDirectory()) {
                if (entry.getName().indexOf(47) != -1) {
                    continue;
                }
                final File f = new File(String.valueOf(path) + "natives" + File.separator + entry.getName());
                if (f.exists() && !f.delete()) {
                    continue;
                }
                final InputStream in = jarFile.getInputStream(jarFile.getEntry(entry.getName()));
                final OutputStream out = new FileOutputStream(String.valueOf(path) + "natives" + File.separator + entry.getName());
                final byte[] buffer = new byte[65536];
                int bufferSize;
                while ((bufferSize = in.read(buffer, 0, buffer.length)) != -1) {
                    out.write(buffer, 0, bufferSize);
                    this.currentSizeExtract += bufferSize;
                    this.percentage = initialPercentage + this.currentSizeExtract * 20 / this.totalSizeExtract;
                    this.subtaskMessage = "Extracting: " + entry.getName() + " " + this.currentSizeExtract * 100 / this.totalSizeExtract + "%";
                }
                validateCertificateChain(certificate, entry.getCertificates());
                in.close();
                out.close();
            }
        }
        this.subtaskMessage = "";
        jarFile.close();
        final File f2 = new File(String.valueOf(path) + nativeJar);
        f2.delete();
    }
    
    protected static void validateCertificateChain(final Certificate[] ownCerts, final Certificate[] native_certs) throws Exception {
        if (ownCerts == null) {
            return;
        }
        if (native_certs == null) {
            throw new Exception("Unable to validate certificate chain. Native entry did not have a certificate chain at all");
        }
        if (ownCerts.length != native_certs.length) {
            throw new Exception("Unable to validate certificate chain. Chain differs in length [" + ownCerts.length + " vs " + native_certs.length + "]");
        }
        for (int i = 0; i < ownCerts.length; ++i) {
            if (!ownCerts[i].equals(native_certs[i])) {
                throw new Exception("Certificate mismatch: " + ownCerts[i] + " != " + native_certs[i]);
            }
        }
    }
    
    protected String getJarName(final URL url) {
        String fileName = url.getFile();
        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf("?"));
        }
        if (fileName.endsWith(".pack.lzma")) {
            fileName = fileName.replaceAll(".pack.lzma", "");
        }
        else if (fileName.endsWith(".pack")) {
            fileName = fileName.replaceAll(".pack", "");
        }
        else if (fileName.endsWith(".lzma")) {
            fileName = fileName.replaceAll(".lzma", "");
        }
        return fileName.substring(fileName.lastIndexOf(47) + 1);
    }
    
    protected String getFileName(final URL url) {
        String fileName = url.getFile();
        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf("?"));
        }
        return fileName.substring(fileName.lastIndexOf(47) + 1);
    }
    
    protected void fatalErrorOccured(final String error, final Exception e) {
        e.printStackTrace();
        this.fatalError = true;
        this.fatalErrorDescription = "Fatal error occured (" + this.state + "): " + error;
        System.out.println(this.fatalErrorDescription);
        System.out.println(this.generateStacktrace(e));
    }
    
    public boolean canPlayOffline() {
        try {
            final String path = AccessController.doPrivileged((PrivilegedExceptionAction<String>)new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    return Util.getWorkingDirectory() + File.separator + "bin" + File.separator;
                }
            });
            File dir = new File(path);
            if (!dir.exists()) {
                return false;
            }
            dir = new File(dir, "version");
            if (!dir.exists()) {
                return false;
            }
            if (dir.exists()) {
                final String version = this.readVersionFile(dir);
                if (version != null && version.length() > 0) {
                    return true;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }
}
