package com.github.cardforge.maven.plugins.android;

import com.android.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * PluginInfo reads plugin.properties which contains filtered
 * values from the build like the GAV coordinates of the plugin itself
 * and provides convenience methods for accessing these properties
 * and related things about the plugin.
 *
 * @author Manfred Moser
 */
public class PluginInfo {
    /**
     * Colon character.
     */
    private static final String COLON = ":";
    /**
     * Properties loaded from plugin.properties.
     */
    private static Properties prop;
    /**
     * Group ID of the plugin.
     */
    private static String groupId;
    /**
     * Artifact ID of the plugin.
     */
    private static String artifactId;
    /**
     * Version of the plugin.
     */
    private static String version;

    static {
        loadProperties();
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private PluginInfo() {
        // no instances
    }

    /**
     * Load the properties from the plugin.properties file.
     */
    private static void loadProperties() {
        prop = new Properties();
        InputStream in = PluginInfo.class.getResourceAsStream("plugin.properties");
        try {
            prop.load(in);
            groupId = prop.getProperty("groupId");
            artifactId = prop.getProperty("artifactId");
            version = prop.getProperty("version");
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("Could not load plugin.properties");
        }
    }

    /**
     * Get the Maven GAV string of the plugin.
     *
     * @return GAV string
     */
    @NonNull
    public static String getGAV() {
        StringBuilder builder = new StringBuilder()
                .append(groupId)
                .append(COLON)
                .append(artifactId)
                .append(COLON)
                .append(version);
        return builder.toString();
    }

    /**
     * @return the groupId of the plugin
     */
    public static String getGroupId() {
        return groupId;
    }

    /**
     * @return the artifactId of the plugin
     */
    public static String getArtifactId() {
        return artifactId;
    }

    /**
     * @return the version of the plugin
     */
    public static String getVersion() {
        return version;
    }

    /**
     * @param goal the goal to be qualified with the plugin coordinates
     * @return the qualified goal string
     */
    @NonNull
    public static String getQualifiedGoal(String goal) {
        StringBuilder builder = new StringBuilder()
                .append(groupId)
                .append(COLON)
                .append(artifactId)
                .append(COLON)
                .append(version)
                .append(COLON)
                .append(goal);
        return builder.toString();
    }
}