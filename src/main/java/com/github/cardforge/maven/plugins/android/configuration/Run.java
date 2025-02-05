package com.github.cardforge.maven.plugins.android.configuration;

import com.github.cardforge.maven.plugins.android.standalonemojos.RunMojo;

/**
 * Configuration for the Run goal.
 *
 * @author Manfred Moser - manfred@simpligility.com
 * @see RunMojo
 */
public class Run {

    /**
     * Mirror of {@link RunMojo}#runDebug
     */
    protected String debug;

    /**
     * @return Mirror of {@link RunMojo}#runDebug
     */
    public String isDebug() {
        return debug;
    }
}
