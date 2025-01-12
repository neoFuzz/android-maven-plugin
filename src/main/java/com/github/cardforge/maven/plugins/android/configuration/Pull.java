package com.github.cardforge.maven.plugins.android.configuration;

/**
 * Configuration for the integration test runs. This class is only the definition of the parameters that are
 * shadowed in
 * {@link AbstractInstrumentationMojo} and used there.
 *
 * @author Manfred Moser - manfred@simpligility.com
 */
public class Pull {
    /**
     * Mirror of {@link PullMojo#source}
     */
    private String source;
    /**
     * Mirror of {@link PullMojo#destination}
     */
    private String destination;

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }
}
