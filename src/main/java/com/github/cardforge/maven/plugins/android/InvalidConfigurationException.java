package com.github.cardforge.maven.plugins.android;

/**
 * Exception for notifying about an invalid plugin configuration.
 *
 * @author Manfred Moser - manfred@simpligility.com
 */
public class InvalidConfigurationException extends RuntimeException {
    /**
     * @param message The message to display to the user
     */
    public InvalidConfigurationException(String message) {
        super(message);
    }
}
