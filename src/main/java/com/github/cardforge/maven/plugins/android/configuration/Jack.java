package com.github.cardforge.maven.plugins.android.configuration;

import com.android.annotations.NonNull;
import com.github.cardforge.maven.plugins.android.compiler.JackCompiler;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.Map;

/**
 * This code is part of a larger Android build system where Jack is an
 * alternative compiler tool.
 * (It's worth noting that Jack was deprecated by Android some time ago).
 */
public class Jack {

    /**
     * Boolean holding if we are enabled or not.
     */
    private final Boolean enabled = false;
    /**
     * @parameter expression="maven.compiler.compilerId"  default-value=""
     */
    @Parameter(property = "maven.compiler.compilerId", defaultValue = "")
    private String mavenCompilerId = "";

    /**
     * Default constructor
     */
    public Jack() {
    }

    /**
     * @param pluginContext the plugin context
     */
    public Jack(@NonNull Map pluginContext) {
        MavenProject project = (MavenProject) pluginContext.get("project");
        mavenCompilerId = project.getProperties().getProperty("maven.compiler.compilerId", "");
    }

    /**
     * @return true if jack is enabled
     */
    public Boolean isEnabled() {
        return enabled || mavenCompilerId.equals(JackCompiler.JACK_COMPILER_ID);
    }
}
