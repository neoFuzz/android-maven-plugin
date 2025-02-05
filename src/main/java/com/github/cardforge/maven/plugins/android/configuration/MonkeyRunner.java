package com.github.cardforge.maven.plugins.android.configuration;

import java.util.List;

/**
 * Configuration for the monkey runner tests runs. This class is only the definition of the parameters that are shadowed
 * in {@code com.jayway.maven.plugins.android.standalonemojos.MonkeyRunner} and used there.
 *
 * @author Stéphane Nicolas - snicolas@octo.com
 */
public class MonkeyRunner {
    /**
     * Mirror of {@code com.jayway.maven.plugins.android.standalonemojos.MonkeyRunner#testSkip}
     */
    private Boolean skip;
    /**
     * Mirror of {@code com.jayway.maven.plugins.android.standalonemojos.MonkeyRunner#programs}
     */
    private List<Program> programs;
    /**
     * Mirror of {@code com.jayway.maven.plugins.android.standalonemojos.MonkeyRunner#plugins}
     */
    private String[] plugins;
    /**
     * Mirror of {@code com.jayway.maven.plugins.android.standalonemojos.MonkeyRunner#createReport}
     */
    private Boolean createReport;
    /**
     * Mirror of
     * {@code com.jayway.maven.plugins.android.standalonemojos.MonkeyRunner#injectDeviceSerialNumberIntoScript}
     */
    private Boolean injectDeviceSerialNumberIntoScript;

    /**
     * @return true if the monkey runner goal is configured to be skipped
     */
    public boolean isSkip() {
        return skip;
    }

    /**
     * @return the list of programs to execute
     */
    public List<Program> getPrograms() {
        return programs;
    }

    /**
     * @return the list of plugins to execute
     */
    public String[] getPlugins() {
        return plugins;
    }

    /**
     * @return true if the monkey runner goal is configured to create a report
     */
    public Boolean isCreateReport() {
        return createReport;
    }

    /**
     * @return true if the monkey runner goal is configured to inject the device serial number into the script
     */
    public Boolean isInjectDeviceSerialNumberIntoScript() {
        return injectDeviceSerialNumberIntoScript;
    }
}
