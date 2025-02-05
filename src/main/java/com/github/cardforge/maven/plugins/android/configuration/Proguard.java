package com.github.cardforge.maven.plugins.android.configuration;

import com.github.cardforge.maven.plugins.android.phase04processclasses.ProguardMojo;

import java.io.File;

/**
 * Configuration container for proguard without default values.
 *
 * @author Matthias Kaeppler
 * @author Manfred Moser
 * @author Michal Harakal
 * @see ProguardMojo
 */
public class Proguard {

    /**
     * Whether ProGuard is enabled or not.
     */
    private Boolean skip;
    /**
     * Path to the ProGuard configuration file (relative to project root).
     */
    private File config;
    private String[] configs;
    private String proguardJarPath;
    private File outputDirectory;
    private String[] jvmArguments;
    private Boolean filterMavenDescriptor;
    private Boolean filterManifest;
    private String customFilter;
    private Boolean includeJdkLibs;
    private String[] options;
    private Boolean attachMap;

    /**
     * @return Whether ProGuard is enabled or not.
     */
    public Boolean isSkip() {
        return skip;
    }

    /**
     * @return Path to the ProGuard configuration file (relative to project root).
     */
    public File getConfig() {
        return config;
    }

    /**
     * @return Path to the ProGuard configuration file (relative to project root).
     */
    public String[] getConfigs() {
        return configs;
    }

    /**
     * @return Path to the ProGuard jar file (relative to project root).
     */
    public String getProguardJarPath() {
        return proguardJarPath;
    }

    /**
     * @return Path to the ProGuard output directory (relative to project root).
     */
    public File getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * @return JVM arguments to pass to ProGuard.
     */
    public String[] getJvmArguments() {
        return jvmArguments;
    }

    /**
     * @return Whether to filter the Maven descriptor from the processed jar.
     */
    public Boolean isFilterMavenDescriptor() {
        return filterMavenDescriptor;
    }

    /**
     * @return Custom filter to apply to the processed jar.
     */
    public String getCustomFilter() {
        return customFilter;
    }

    /**
     * @return if filtering manifest is enabled or not
     */
    public Boolean isFilterManifest() {
        return filterManifest;
    }

    /**
     * @return Whether to include the JDK libraries in the processed jar.
     */
    public Boolean isIncludeJdkLibs() {
        return includeJdkLibs;
    }

    /**
     * @return the options to pass to proguard
     */
    public String[] getOptions() {
        return options;
    }
}
