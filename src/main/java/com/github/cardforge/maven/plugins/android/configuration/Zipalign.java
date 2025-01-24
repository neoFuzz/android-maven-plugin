package com.github.cardforge.maven.plugins.android.configuration;

import com.github.cardforge.maven.plugins.android.standalonemojos.ZipalignMojo;

/**
 * Configuration for the zipalign command. This class is only the definition of the parameters that are shadowed in
 * {@link ZipalignMojo} and used there.
 *
 * @author Manfred Moser - manfred@simpligility.com
 */
public class Zipalign {
    /**
     * Mirror of {@link ZipalignMojo}#zipalignSkip
     */
    private Boolean skip;
    /**
     * Mirror of {@link ZipalignMojo}#zipalignVerbose
     */
    private Boolean verbose;
    /**
     * Mirror of {@link ZipalignMojo}#zipalignInputApk
     */
    private String inputApk;
    /**
     * Mirror of {@link ZipalignMojo}#zipalignOutputApk
     */
    private String outputApk;

    private String classifier;


    public Boolean isSkip() {
        return skip;
    }

    public Boolean isVerbose() {
        return verbose;
    }

    public String getInputApk() {
        return inputApk;
    }

    public String getOutputApk() {
        return outputApk;
    }

    public String getClassifier() {
        return classifier;
    }

}
