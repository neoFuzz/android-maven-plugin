package com.github.cardforge.maven.plugins.android.configuration;

/**
 * Configuration for the Run goal.
 *
 * @author Manfred Moser - manfred@simpligility.com
 * @see RunMojo
 */
public class Run
{

    /**
     * Mirror of {@link RunMojo#runDebug}
     */
    protected String debug;

    public String isDebug()
    {
        return debug;
    }
}
