package com.github.cardforge.config;

import com.github.neofuzz.config.ConfigHandler;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class ConfigHandlerTest {

    private DummyMojo mojo = new DummyMojo();

    @Mock
    private MavenSession session;

    @Mock
    private MavenExecutionRequest request;

    @Mock
    private MavenProject project;

    private MojoExecution execution;

    @BeforeEach
    public void setUp() {
        openMocks(this);
        // Mock the session request to return a mock execution request
        when(session.getRequest()).thenReturn(request);

        // Mock project properties (system properties)
        Properties properties = new Properties();
        properties.put("key", "value");
        when(project.getProperties()).thenReturn(properties);

        // Mock session to return the current project
        when(session.getCurrentProject()).thenReturn(project);

        // Mock the system properties
        when(session.getSystemProperties()).thenReturn(properties);
        when(session.getUserProperties()).thenReturn(properties);

        // Setup MojoExecution
        MojoDescriptor mojoDesc = new MojoDescriptor();
        this.execution = new MojoExecution(mojoDesc);
    }

    @Test
    public void testParseConfigurationDefault() {
        ConfigHandler configHandler = new ConfigHandler(mojo, this.session, this.execution);
        configHandler.parseConfiguration();
        assertTrue(mojo.getParsedBooleanValue());
    }

    @Test
    public void testParseConfigurationFromConfigPojo() {
        mojo.setConfigPojo(new DummyConfigPojo("from config pojo", null));
        ConfigHandler configHandler = new ConfigHandler(mojo, this.session, this.execution);
        configHandler.parseConfiguration();
        assertEquals("from config pojo", mojo.getParsedStringValue());
    }

    @Test
    public void testParseConfigurationFromMaven() {
        mojo.setConfigPojoStringValue("maven value");
        ConfigHandler configHandler = new ConfigHandler(mojo, this.session, this.execution);
        configHandler.parseConfiguration();
        assertEquals("maven value", mojo.getParsedStringValue());
    }

    @Test
    public void testParseConfigurationDefaultMethodValue() {
        ConfigHandler configHandler = new ConfigHandler(mojo, this.session, this.execution);
        configHandler.parseConfiguration();
        assertArrayEquals(new String[]{"a", "b"}, mojo.getParsedMethodValue());
    }
}
