package com.github.cardforge.maven.plugins.android.configuration;

/**
 * ValidationResponse wraps a validation message and result flag
 * allows the using class to decide how to react to a validation
 * failure.
 *
 * @author Manfred Moser - manfred@simpligility.com
 * @see DeployApk
 */
public final class ValidationResponse {
    private final boolean valid;
    private final String message;

    /**
     * @param valid   boolean flag indicating if the validation was successful or not
     * @param message String message to be displayed if validation failed
     */
    public ValidationResponse(final boolean valid, final String message) {
        this.valid = valid;
        this.message = message;
    }

    /**
     * @return <code>true</code> if the validation was successful
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * @return <code>String</code> with the validation message
     */
    public String getMessage() {
        return message;
    }
}
