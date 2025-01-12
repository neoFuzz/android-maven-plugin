package com.github.cardforge.maven.plugins.android.configuration;


/**
 * Configuration for the ui automator test runs. This class is only the definition of the parameters that are shadowed
 * in {@link UIAutomatorMojo} and used there.
 *
 * @author Stéphane Nicolas - snicolas@octo.com
 */
public class UIAutomator {
    /**
     * Mirror of {@link UIAutomatorMojo#testSkip}
     */
    private Boolean skip;
    /**
     * Mirror of {@link UIAutomatorMojo#jarFile}
     */
    private String jarFile;
    /**
     * Mirror of {@link UIAutomatorMojo#testClassOrMethods}
     */
    private String[] testClassOrMethods;
    /**
     * Mirror of {@link UIAutomatorMojo#noHup}
     */
    private Boolean noHup = false;
    /**
     * Mirror of {@link UIAutomatorMojo#debug}
     */
    private Boolean debug = false;
    /**
     * Mirror of {@link UIAutomatorMojo#useDump}
     */
    private Boolean useDump = false;
    /**
     * Mirror of {@link UIAutomatorMojo#dumpFilePath}
     */
    private String dumpFilePath;
    /**
     * Mirror of {@link UIAutomatorMojo#createReport}
     */
    private Boolean createReport;
    /**
     * Mirror of {@link UIAutomatorMojo#reportSuffix}
     */
    private String reportSuffix;
    /**
     * Mirror of {@link UIAutomatorMojo#takeScreenshotOnFailure}
     */
    private Boolean takeScreenshotOnFailure;
    /**
     * Mirror of {@link UIAutomatorMojo#screenshotsPathOnDevice}
     */
    private String screenshotsPathOnDevice;
    /**
     * Mirror of {@link UIAutomatorMojo#parameterPrefix}
     */
    private String propertiesKeyPrefix;

    public Boolean isSkip() {
        return skip;
    }

    public String getJarFile() {
        return jarFile;
    }

    public String[] getTestClassOrMethods() {
        return testClassOrMethods;
    }

    public Boolean getNoHup() {
        return noHup;
    }

    public Boolean getDebug() {
        return debug;
    }

    public Boolean getUseDump() {
        return useDump;
    }

    public String getDumpFilePath() {
        return dumpFilePath;
    }

    public Boolean isCreateReport() {
        return createReport;
    }

    public String getReportSuffix() {
        return reportSuffix;
    }

    public Boolean isTakeScreenshotOnFailure() {
        return takeScreenshotOnFailure;
    }

    public String getScreenshotsPathOnDevice() {
        return screenshotsPathOnDevice;
    }

    public String getPropertiesKeyPrefix() {
        return propertiesKeyPrefix;
    }
}
