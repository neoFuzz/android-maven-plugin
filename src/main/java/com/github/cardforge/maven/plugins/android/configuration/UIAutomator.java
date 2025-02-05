package com.github.cardforge.maven.plugins.android.configuration;


import com.github.cardforge.maven.plugins.android.standalonemojos.UIAutomatorMojo;

/**
 * Configuration for the ui automator test runs. This class is only the definition of the parameters that are shadowed
 * in {@link UIAutomatorMojo} and used there.
 *
 * @author Stéphane Nicolas - snicolas@octo.com
 */
public class UIAutomator {
    /**
     * Mirror of {@link UIAutomatorMojo}#testSkip
     */
    private Boolean skip;
    /**
     * Mirror of {@link UIAutomatorMojo}#jarFile
     */
    private String jarFile;
    /**
     * Mirror of {@link UIAutomatorMojo}#testClassOrMethods
     */
    private String[] testClassOrMethods;
    /**
     * Mirror of {@link UIAutomatorMojo}#noHup
     */
    private Boolean noHup = false;
    /**
     * Mirror of {@link UIAutomatorMojo}#debug
     */
    private Boolean debug = false;
    /**
     * Mirror of {@link UIAutomatorMojo}#useDump
     */
    private Boolean useDump = false;
    /**
     * Mirror of {@link UIAutomatorMojo}#dumpFilePath
     */
    private String dumpFilePath;
    /**
     * Mirror of {@link UIAutomatorMojo}#createReport
     */
    private Boolean createReport;
    /**
     * Mirror of {@link UIAutomatorMojo}#reportSuffix
     */
    private String reportSuffix;
    /**
     * Mirror of {@link UIAutomatorMojo}#takeScreenshotOnFailure
     */
    private Boolean takeScreenshotOnFailure;
    /**
     * Mirror of {@link UIAutomatorMojo}#screenshotsPathOnDevice
     */
    private String screenshotsPathOnDevice;
    /**
     * Mirror of {@link UIAutomatorMojo}#parameterPrefix
     */
    private String propertiesKeyPrefix;

    /**
     * @return the skip
     */
    public Boolean isSkip() {
        return skip;
    }

    /**
     * @return the jarFile
     */
    public String getJarFile() {
        return jarFile;
    }

    /**
     * @return the testClassOrMethods
     */
    public String[] getTestClassOrMethods() {
        return testClassOrMethods;
    }

    /**
     * @return the noHup
     */
    public Boolean getNoHup() {
        return noHup;
    }

    /**
     * @return the debug
     */
    public Boolean getDebug() {
        return debug;
    }

    /**
     * @return the useDump
     */
    public Boolean getUseDump() {
        return useDump;
    }

    /**
     * @return the dumpFilePath
     */
    public String getDumpFilePath() {
        return dumpFilePath;
    }

    /**
     * @return the createReport
     */
    public Boolean isCreateReport() {
        return createReport;
    }

    /**
     * @return the report suffix
     */
    public String getReportSuffix() {
        return reportSuffix;
    }

    /**
     * @return the takeScreenshotOnFailure
     */
    public Boolean isTakeScreenshotOnFailure() {
        return takeScreenshotOnFailure;
    }

    /**
     * @return the screenshotsPathOnDevice
     */
    public String getScreenshotsPathOnDevice() {
        return screenshotsPathOnDevice;
    }

    /**
     * @return the propertiesKeyPrefix
     */
    public String getPropertiesKeyPrefix() {
        return propertiesKeyPrefix;
    }
}
