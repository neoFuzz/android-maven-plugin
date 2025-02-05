package com.github.cardforge.maven.plugins.android.configuration;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Interface for parsing version names into version elements.
 *
 * @author Wang Xuerui  - idontknw.wang@gmail.com
 */
public interface VersionElementParser {
    /**
     * @param versionName the version name to parse
     * @return an array of version elements
     * @throws MojoExecutionException if the version name is invalid
     */
    int[] parseVersionElements(final String versionName) throws MojoExecutionException;
}
