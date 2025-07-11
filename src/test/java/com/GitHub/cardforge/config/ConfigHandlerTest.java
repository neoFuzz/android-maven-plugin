package com.github.cardforge.config;

import com.github.cardforge.maven.plugins.android.config.ConfigHandler;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class ConfigHandlerTest {

    private DummyMojo mojo = new DummyMojo();

    private MavenSession session;
    private MojoExecution execution;

    @Before
    public void setUp() {
        // Create mock objects
        session = createNiceMock(MavenSession.class);
        MavenExecutionRequest request = createNiceMock(MavenExecutionRequest.class);
        MavenProject project = createNiceMock(MavenProject.class);

        // Mock the session request to return a mock execution request
        expect(session.getRequest()).andReturn(request).anyTimes();

        // Mock project properties (system properties)
        Properties properties = new Properties();
        properties.put("key", "value");
        expect(project.getProperties()).andReturn(properties).anyTimes(); // Return properties for project

        // Mock session to return the current project
        expect(session.getCurrentProject()).andReturn(project).anyTimes();

        // Mock the system properties (we assume they are the same as project properties)
        expect(session.getSystemProperties()).andReturn(properties).anyTimes();
        expect(session.getUserProperties()).andReturn(properties).anyTimes();

        // Mock MojoExecution setup
        MojoDescriptor mojoDesc = new MojoDescriptor();
        this.execution = new MojoExecution(mojoDesc);

        // Replay the mocks
        replay(session, request, project);
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
