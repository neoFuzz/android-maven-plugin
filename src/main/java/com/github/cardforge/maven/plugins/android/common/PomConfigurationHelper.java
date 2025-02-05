package com.github.cardforge.maven.plugins.android.common;

import com.android.annotations.NonNull;
import com.github.cardforge.maven.plugins.android.PluginInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;

/**
 * A helper class to access plugin configuration from the pom.
 *
 * @author Benoit Billington
 * @author Manfred Moser
 */
public final class PomConfigurationHelper {
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private PomConfigurationHelper() {
        // private constructor
    }

    /**
     * @param project      the maven project
     * @param parameter    the name of the parameter to retrieve
     * @param defaultValue the default value to return if the parameter is not found
     * @return the value of the parameter, or the default value if the parameter is not found
     */
    public static String getPluginConfigParameter(@NonNull MavenProject project, String parameter, String defaultValue) {
        String value = null;
        for (Plugin plugin : project.getBuild().getPlugins()) {
            if (plugin.getArtifactId().equals(PluginInfo.getArtifactId())) {
                PlexusConfiguration configuration = getMojoConfiguration(plugin);
                if (configuration != null) {
                    PlexusConfiguration param = configuration.getChild(parameter);
                    if (param != null) {
                        value = param.getValue();
                    }
                }
            }
        }
        // if we got nothing, fall back to the default value
        return (StringUtils.isEmpty(value)) ? defaultValue : value;
    }

    /**
     * @param project      the maven project
     * @param parameter    the name of the parameter to retrieve
     * @param defaultValue the default value to return if the parameter is not found
     * @return the value of the parameter, or the default value if the parameter is not found
     */
    public static boolean getPluginConfigParameter(MavenProject project, String parameter, boolean defaultValue) {
        String value = getPluginConfigParameter(project, parameter, Boolean.toString(defaultValue));
        return Boolean.parseBoolean(value);
    }

    /**
     * @param plugin the plugin to retrieve the configuration from
     * @return the configuration of the plugin, or null if it cannot be retrieved
     */
    private static PlexusConfiguration getMojoConfiguration(@NonNull Plugin plugin) {
        PlexusConfiguration configuration = null;
        // Try to retrieve the configuration from the plugin
        if (plugin.getConfiguration() instanceof XmlPlexusConfiguration xpc) {
            configuration = xpc;
        }
        // Fall back to checking plugin executions
        if (configuration == null && !plugin.getExecutions().isEmpty() &&
                plugin.getExecutions().get(0).getConfiguration() instanceof XmlPlexusConfiguration xpc) {
            configuration = xpc;
        }

        return configuration;
    }
}
