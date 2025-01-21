package com.github.cardforge.maven.plugins.android.configuration;

import com.android.annotations.NonNull;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * VersionElementParser implementing the old version generator behavior.
 *
 * @author Wang Xuerui  - idontknw.wang@gmail.com
 */
public class SimpleVersionElementParser implements VersionElementParser {

    /**
     * @param versionName The version name to parse.
     * @return The parsed version elements.
     * @throws MojoExecutionException If the version name is invalid.
     */
    @Override
    public int[] parseVersionElements(@NonNull final String versionName) throws MojoExecutionException {
        final String[] versionNameElements = versionName.replaceAll("[^0-9.]", "").split("\\.");
        int[] result = new int[versionNameElements.length];

        for (int i = 0; i < versionNameElements.length; i++) {
            result[i] = Integer.parseInt(versionNameElements[i]);
        }

        return result;
    }
}
