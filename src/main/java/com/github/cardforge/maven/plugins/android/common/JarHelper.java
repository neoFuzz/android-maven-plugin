package com.github.cardforge.maven.plugins.android.common;

import com.android.annotations.NonNull;
import org.codehaus.plexus.util.IOUtil;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Helper class to deal with jar files.
 *
 * @author Johan Lindquist
 */
public class JarHelper {

    /**
     * Un-jars the specified jar file into the specified directory
     *
     * @param jarFile         The jar file to un-jar
     * @param outputDirectory The directory into which to un-jar the jar file
     * @param unjarListener   The listener to determine which entries to include
     * @throws IOException Thrown if an error occurs while un-jarring the jar file
     */
    public static void unjar(@NonNull JarFile jarFile, File outputDirectory, UnjarListener unjarListener) throws IOException {
        for (Enumeration<JarEntry> en = jarFile.entries(); en.hasMoreElements(); ) {
            JarEntry entry = en.nextElement();
            File entryFile = new File(outputDirectory, entry.getName());
            if (!entryFile.toPath().normalize().startsWith(outputDirectory.toPath().normalize())) {
                throw new IOException("Bad zip entry");
            }
            if (unjarListener.include(entry)) {
                // Create the output directory if need be
                if (!entryFile.getParentFile().exists() &&
                        !entryFile.getParentFile().mkdirs()) {
                        throw new IOException("Error creating output directory: " + entryFile.getParentFile());
                    }


                // If the entry is an actual file, unzip that too
                if (!entry.isDirectory()) {
                    try (final InputStream in = jarFile.getInputStream(entry)) {
                        try (final OutputStream out = new FileOutputStream(entryFile)) {
                            IOUtil.copy(in, out);
                        }
                    }
                }
            }
        }
    }

    /**
     * Listener for jar extraction.
     */
    public interface UnjarListener {
        boolean include(JarEntry jarEntry);
    }

}
