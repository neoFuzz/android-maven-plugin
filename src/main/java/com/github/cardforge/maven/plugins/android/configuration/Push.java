package com.github.cardforge.maven.plugins.android.configuration;

import com.github.cardforge.maven.plugins.android.standalonemojos.PushMojo;

/**
 * Configuration for the integration test runs. This class is only the definition of the parameters that are
 * shadowed in
 * {@link PushMojo} and used there.
 *
 * @author Manfred Moser - manfred@simpligility.com
 */
public class Push {
    /**
     * Mirror of {@link PushMojo}#source
     */
    private String source;
    /**
     * Mirror of {@link PushMojo}#destination
     */
    private String destination;

    /**
     * @return Mirror of {@link PushMojo}#source
     */
    public String getSource() {
        return source;
    }

    /**
     * @return Mirror of {@link PushMojo}#destination
     */
    public String getDestination() {
        return destination;
    }
}
