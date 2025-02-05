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
    /**
     * Mirror of {@link ZipalignMojo}#zipalignClassifier
     */
    private String classifier;


    /**
     * @return return skip
     */
    public Boolean isSkip() {
        return skip;
    }

    /**
     * @return return verbose
     */
    public Boolean isVerbose() {
        return verbose;
    }

    /**
     * @return return inputApk
     */
    public String getInputApk() {
        return inputApk;
    }

    /**
     * @return if outputApk is null, return inputApk with -aligned.apk appended
     * if outputApk is not null, return outputApk
     */
    public String getOutputApk() {
        return outputApk;
    }

    /**
     * @return if classifier is null, return empty string
     * if classifier is not null, return classifier with -aligned.apk appended
     */
    public String getClassifier() {
        return classifier;
    }

}
