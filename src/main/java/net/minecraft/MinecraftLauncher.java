package net.minecraft;

import java.util.*;

public class MinecraftLauncher {
    // * 4, current minecraft should need more ram, not sure though
    private static final int MIN_HEAP = 512 * 4;
    private static final int RECOMMENDED_HEAP = 1024 * 4;

    public static void main(final String[] args) {
        final float heapSizeMegs = (float) (Runtime.getRuntime().maxMemory() / 1024L / 1024L);
        if (heapSizeMegs > 511.0f) {
            LauncherFrame.main(args);
        } else {
            try {
                final String pathToJar = MinecraftLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                final ArrayList<String> params = new ArrayList<String>();
                params.add("javaw");
                params.add("-Xmx1024m");
                params.add("-Dsun.java2d.noddraw=true");
                params.add("-Dsun.java2d.d3d=false");
                params.add("-Dsun.java2d.opengl=false");
                params.add("-Dsun.java2d.pmoffscreen=false");
                params.add("-classpath");
                params.add(pathToJar);
                params.add("net.minecraft.LauncherFrame");
                final ProcessBuilder pb = new ProcessBuilder(params);
                final Process process = pb.start();
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
                LauncherFrame.main(args);
            }
        }
    }
}
