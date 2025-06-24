package com.github.cardforge.maven.plugins.android.standalonemojos;

import com.github.cardforge.maven.plugins.android.AbstractEmulatorMojo;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;

import javax.inject.Inject;

/**
 * EmulatorStopeAllMojo will stop all attached devices.
 *
 * @author Bryan O'Neil - bryan.oneil@hotmail.com
 */
@SuppressWarnings("unused") // used in Maven goals
@Mojo(name = "emulator-stop-all", requiresProject = false)
public class EmulatorStopAllMojo extends AbstractEmulatorMojo {

    /**
     * {@inheritDoc}
     */
    @Inject
    protected EmulatorStopAllMojo(ArtifactResolver artifactResolver, ArtifactHandler artHandler,
                                  MavenProjectHelper projectHelper, DependencyGraphBuilder dependencyGraphBuilder) {
        super(artifactResolver, artHandler, projectHelper, dependencyGraphBuilder);
    }

    /**
     * Stop all attached emulators.
     *
     * @throws MojoExecutionException if the execution fails
     * @throws MojoFailureException   if the execution fails
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        stopAndroidEmulators();
    }
}
