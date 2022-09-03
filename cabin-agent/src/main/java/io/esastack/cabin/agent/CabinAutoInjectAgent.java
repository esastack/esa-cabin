package io.esastack.cabin.agent;

import io.esastack.cabin.common.constant.Constants;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CabinAutoInjectAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        try {
            appendCabinCoreDependencies(inst);
            inst.addTransformer(new CabinInjectClassFileTransformer());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void appendCabinCoreDependencies(final Instrumentation inst) {
        try {
            final List<JarFile> cabinJars = extractCabinJars();
            for (JarFile jarFile : cabinJars) {
                inst.appendToSystemClassLoaderSearch(jarFile);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to extract Cabin core JarFiles!", e);
        }
    }

    private static List<JarFile> extractCabinJars() throws IOException {
        final CodeSource codeSource = CabinAutoInjectAgent.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            throw new IllegalStateException("could not get agent jar location");
        }
        final File javaagentFile;
        try {
            javaagentFile = new File(codeSource.getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Failed to create java agent file: " + codeSource.getLocation().toExternalForm());
        }
        if (!javaagentFile.isFile()) {
            throw new IllegalStateException(
                    "agent jar location doesn't appear to be a file: " + javaagentFile.getAbsolutePath());
        }
        final String tmpDirName = "cabin-tmp/";
        final File tmpDir = new File(tmpDirName);
        tmpDir.deleteOnExit();
        tmpDir.mkdir();
        final JarFile agentJar = new JarFile(javaagentFile, false);
        final Enumeration<JarEntry> entries = agentJar.entries();
        final List<JarFile> cabinJars = new ArrayList<>(8);
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            if (entry.getName().startsWith("lib") && !entry.isDirectory()) {
                final InputStream in = agentJar.getInputStream(entry);
                final String tmpFileName = tmpDirName + entry.getName().substring(4);
                final File tmpFile = new File(tmpFileName);
                tmpFile.deleteOnExit();
                tmpFile.createNewFile();
                final OutputStream out = new FileOutputStream(tmpFile);
                byte[] buf = new byte[4096];
                int len = in.read(buf);
                while (len != -1) {
                    out.write(buf, 0, len);
                    len = in.read(buf);
                }
                out.flush();
                out.close();
                in.close();
                cabinJars.add(new JarFile(tmpFile));
            }
        }
        return cabinJars;
    }

}


