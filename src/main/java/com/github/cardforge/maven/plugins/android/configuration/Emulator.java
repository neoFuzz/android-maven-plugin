package com.github.cardforge.maven.plugins.android.configuration;


import com.github.cardforge.maven.plugins.android.AbstractAndroidMojo;

/**
 * Configuration for the Android Emulator. This class is only the definition of the parameters that are shadowed in
 * {@link AbstractAndroidMojo} and used there.
 *
 * @author Manfred Moser - manfred@simpligility.com
 */
public class Emulator {
    /**
     * Mirror of {@link AbstractEmulatorMojo#emulatorAvd}
     */
    private String avd;

    /**
     * Mirror of {@link AbstractEmulatorMojo#emulatorWait}
     */
    private String wait;

    /**
     * Mirror of {@link AbstractEmulatorMojo#emulatorOptions}
     */
    private String options;

    /**
     * Override default emulator executable
     */
    private String executable;
    private String location;

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @return the Android Virtual Device name
     */
    public String getAvd() {
        return avd;
    }

    /**
     * @return the wait time
     */
    public String getWait() {
        return wait;
    }

    /**
     * @return the emulator options
     */
    public String getOptions() {
        return options;
    }

    /**
     * @return the emulator executable
     */
    public String getExecutable() {
        return executable;
    }
}
