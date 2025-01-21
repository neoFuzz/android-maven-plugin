package com.github.cardforge.maven.plugins.android.standalonemojos;

import com.github.cardforge.maven.plugins.android.AbstractEmulatorMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * EmulatorStartMojo can stop the Android Emulator with a specified Android Virtual Device (avd).
 *
 * @author Manfred Moser - manfred@simpligility.com
 */
@SuppressWarnings("unused") // used in Maven goals
@Mojo(name = "emulator-stop", requiresProject = false)
public class EmulatorStopMojo extends AbstractEmulatorMojo {

    /**
     * Stop the emulator(s).
     *
     * @throws MojoExecutionException if the execution fails
     * @throws MojoFailureException   if the execution fails
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        stopAndroidEmulator();
    }
}
