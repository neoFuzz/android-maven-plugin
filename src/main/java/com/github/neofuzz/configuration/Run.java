package com.github.neofuzz.configuration;

import com.github.neofuzz.standalonemojos.RunMojo;

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
