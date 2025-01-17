package com.github.cardforge.maven.plugins.android.configuration;

/**
 * Configuration for the integration test runs. This class is only the definition of the parameters that are
 * shadowed in
 * {@link PushMojo} and used there.
 *
 * @author Manfred Moser - manfred@simpligility.com
 */
public class Push
{
    /**
     * Mirror of {@link PushMojo#source}
     */
    private String source;
    /**
     * Mirror of {@link PushMojo#destination}
     */
    private String destination;

    public String getSource()
    {
        return source;
    }

    public String getDestination()
    {
        return destination;
    }
}
