package com.github.cardforge.maven.plugins.android.standalonemojos;

import com.github.cardforge.maven.plugins.android.AbstractAndroidMojo;
import com.github.cardforge.maven.plugins.android.CommandExecutor;
import com.github.cardforge.maven.plugins.android.ExecutionException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Disconnect external IP addresses from the ADB server.
 *
 * @author demey.emmanuel@gmail.com
 */
@SuppressWarnings("unused") // used in Maven goals
@Mojo(name = "disconnect", requiresProject = false)
public class DisconnectMojo extends AbstractAndroidMojo {
    /**
     * {@inheritDoc}
     */
    @Inject
    protected DisconnectMojo(ArtifactResolver artifactResolver, ArtifactHandler artHandler,
                             MavenProjectHelper projectHelper, DependencyGraphBuilder dependencyGraphBuilder) {
        super(artifactResolver, artHandler, projectHelper, dependencyGraphBuilder);
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (ips.length > 0) {
            CommandExecutor executor = CommandExecutor.Factory.createDefaultCommandExecutor();
            executor.setLogger(this.getLog());

            for (String ip : ips) {
                getLog().debug("Disconnecting " + ip);


                // It would be better to use the AndroidDebugBridge class 
                // rather than calling the command line tool
                String command = getAndroidSdk().getAdbPath();

                List<String> parameters = new ArrayList<>();
                parameters.add("disconnect");
                parameters.add(ip);

                try {
                    executor.setCaptureStdOut(true);
                    executor.executeCommand(command, parameters, false);
                } catch (ExecutionException e) {
                    throw new MojoExecutionException(String.format("Can not disconnect %s", ip), e);
                }
            }
        }
    }
}
