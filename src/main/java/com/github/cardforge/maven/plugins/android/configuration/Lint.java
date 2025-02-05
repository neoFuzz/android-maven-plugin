package com.github.cardforge.maven.plugins.android.configuration;

import com.github.cardforge.maven.plugins.android.standalonemojos.LintMojo;

/**
 * Configuration for the lint command. This class is only the definition of the parameters that are shadowed in
 * {@link LintMojo} and used there.
 *
 * @author Stéphane Nicolas snicolas@octo.com
 * @author Manfred Moser - manfred@simpligility.com
 * @see LintMojo
 */
public class Lint {
    private Boolean failOnError;
    private Boolean skip;
    private Boolean legacy;
    private Boolean quiet;

    // ---------------
    // Enabled Checks
    // ---------------
    private Boolean ignoreWarnings;
    private Boolean warnAll;
    private Boolean warningsAsErrors;
    private String config;

    // ---------------
    // Output Options
    // ---------------
    private Boolean fullPath;
    private Boolean showAll;
    private Boolean disableSourceLines;
    private String url;

    private Boolean enableHtml;
    private String htmlOutputPath;
    private Boolean enableSimpleHtml;
    private String simpleHtmlOutputPath;
    private Boolean enableXml;
    private String xmlOutputPath;

    // ---------------
    // Project Options
    // ---------------
    private Boolean enableSources;
    private String sources;
    private Boolean enableClasspath;
    private String classpath;
    private Boolean enableLibraries;
    private String libraries;

    // ---------------
    // Getters
    // ---------------

    /**
     * @return the failOnError
     */
    public final Boolean isFailOnError() {
        return failOnError;
    }

    /**
     * @return the skip
     */
    public final Boolean isSkip() {
        return skip;
    }

    /**
     * @return the quiet
     */
    public final Boolean isQuiet() {
        return quiet;
    }

    /**
     * @return the legacy
     */
    public final Boolean isLegacy() {
        return legacy;
    }

    /**
     * @return the ignoreWarnings
     */
    public final Boolean isIgnoreWarnings() {
        return ignoreWarnings;
    }

    /**
     * @return the warnAll
     */
    public final Boolean isWarnAll() {
        return warnAll;
    }

    /**
     * @return the warningsAsErrors
     */
    public final Boolean isWarningsAsErrors() {
        return warningsAsErrors;
    }

    /**
     * @return the config
     */
    public final String getConfig() {
        return config;
    }

    /**
     * @return the fullPath
     */
    public final Boolean isFullPath() {
        return fullPath;
    }

    /**
     * @return the showAll
     */
    public final Boolean getShowAll() {
        return showAll;
    }

    /**
     * @return the disableSourceLines
     */
    public final Boolean isDisableSourceLines() {
        return disableSourceLines;
    }

    /**
     * @return the url
     */
    public final String getUrl() {
        return url;
    }

    /**
     * @return the enableHtml
     */
    public final Boolean isEnableHtml() {
        return enableHtml;
    }

    /**
     * @return the htmlOutputPath
     */
    public final String getHtmlOutputPath() {
        return htmlOutputPath;
    }

    /**
     * @return the enableSimpleHtml
     */
    public final Boolean isEnableSimpleHtml() {
        return enableSimpleHtml;
    }

    /**
     * @return the simpleHtmlOutputPath
     */
    public final String getSimpleHtmlOutputPath() {
        return simpleHtmlOutputPath;
    }

    /**
     * @return the enableXml
     */
    public final Boolean isEnableXml() {
        return enableXml;
    }

    /**
     * @return the xmlOutputPath
     */
    public final String getXmlOutputPath() {
        return xmlOutputPath;
    }

    /**
     * @return the enableSources
     */
    public Boolean getEnableSources() {
        return enableSources;
    }

    /**
     * @return the sources
     */
    public final String getSources() {
        return sources;
    }

    /**
     * @return the enableClasspath
     */
    public Boolean getEnableClasspath() {
        return enableClasspath;
    }

    /**
     * @return the classpath
     */
    public final String getClasspath() {
        return classpath;
    }

    /**
     * @return the enableLibraries
     */
    public Boolean getEnableLibraries() {
        return enableLibraries;
    }

    /**
     * @return the libraries
     */
    public final String getLibraries() {
        return libraries;
    }
}
