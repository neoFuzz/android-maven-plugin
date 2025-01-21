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
    private static final String COLON = ":";
    private static Properties prop;
    private static String groupId;
    private static String artifactId;
    private static String version;

    static {
        loadProperties();
    }

    private PluginInfo() {
        // no instances
    }

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

    public static String getGroupId() {
        return groupId;
    }

    public static String getArtifactId() {
        return artifactId;
    }

    public static String getVersion() {
        return version;
    }

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