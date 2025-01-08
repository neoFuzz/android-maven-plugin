package com.github.cardforge.maven.plugins.android.configuration;




/**
 * Configuration for the Android Emulator. This class is only the definition of the parameters that are shadowed in
 * {@link AbstractAndroidMojo} and used there.
 *
 * @author Manfred Moser - manfred@simpligility.com
 */
public class Emulator
{
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

    public String getLocation()
    {
        return location;
    }

    private String location;

    public String getAvd()
    {
        return avd;
    }

    public String getWait()
    {
        return wait;
    }

    public String getOptions()
    {
        return options;
    }

    public String getExecutable()
    {
        return executable;
    }
}
