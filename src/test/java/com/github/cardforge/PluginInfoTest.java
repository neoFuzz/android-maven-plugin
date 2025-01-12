package com.github.cardforge;

import com.github.cardforge.maven.plugins.android.PluginInfo;
import org.junit.Test;

import static org.junit.Assert.*;

public class PluginInfoTest {

    @Test
    public void confirmGroupId() {
        assertEquals("com.github.cardforge.maven.plugins", PluginInfo.getGroupId());
    }

    @Test
    public void confirmArtifactId() {
        assertEquals("android-maven-plugin", PluginInfo.getArtifactId());
    }

    @Test
    public void confirmVersion() {
        assertNotNull(PluginInfo.getVersion());
    }

    @Test
    public void confirmGav() {
        assertTrue(PluginInfo.getGAV()
                .startsWith("com.github.cardforge.maven.plugins:android-maven-plugin:"));
    }
}