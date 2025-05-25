package com.github.cardforge.standalonemojos;

import com.github.cardforge.AbstractAndroidMojoTestCase;
import com.github.cardforge.maven.plugins.android.config.ConfigHandler;
import com.github.cardforge.maven.plugins.android.standalonemojos.MonkeyMojo;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * Test the Monkey mojo. Tests options' default values and parsing. We do not test the command line that is passed to
 * the adb bridge, it should be possible to mock it though.
 *
 * @author Stéphane Nicolas - snicolas@octo.com
 */
@RunWith(MockitoJUnitRunner.class)
@Ignore("This test has to be migrated to be an IntegrationTest using AbstractAndroidMojoIntegrationTest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MonkeyMojoTest extends AbstractAndroidMojoTestCase<MonkeyMojo> {
    @Mock
    private MavenProject project;


    @Override
    public String getPluginGoalName() {
        return "monkey";
    }

    @Override
    protected Class<MonkeyMojo> getMojoClass() {
        return null;
    }

    @BeforeAll
    public void setup() {
        openMocks(this);
    }

    /**
     * Tests all options, checks if their default values are correct.
     *
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void testDefaultMonkeyConfig() throws Exception {
        // given
        MonkeyMojo mojo = createMojo("monkey-config-project0");
        final ConfigHandler cfh = new ConfigHandler(mojo, this.session, this.execution);
        cfh.parseConfiguration();

        // when
        Boolean automatorSkip = getFieldValue(mojo, "parsedSkip");

        // then
        assertTrue(automatorSkip, "Monkey skip parameter should be true");
    }

    /**
     * Tests all options, checks if their default values are correct.
     *
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void testDefaultUnskippedMonkeyConfig() throws Exception {
        // given
        MonkeyMojo mojo = createMojo("monkey-config-project1");

        setupProjectMock("monkey-config-project1-15.4.3.1011");
        setInternalState(mojo, "project", project);

        // when
        final ConfigHandler cfh = new ConfigHandler(mojo, this.session, this.execution);
        cfh.parseConfiguration();

        // then
        assertCustomConfiguration(mojo);
    }

    /**
     * Tests all options, checks if they are parsed correctly.
     *
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void testCustomMonkeyConfig() throws Exception {
        // given
        MonkeyMojo mojo = createMojo("monkey-config-project2");
        setupProjectMock("ui-automator-config-project1-15.4.3.1011");
        setInternalState(mojo, "project", project);

        // when
        final ConfigHandler cfh = new ConfigHandler(mojo, this.session, this.execution);
        cfh.parseConfiguration();

        // then
        assertCustomConfiguration(mojo);
    }

    @SuppressWarnings("unchecked")
    private <T> T getFieldValue(Object object, String fieldName) {
        try {
            return (T) object.getClass().getDeclaredField(fieldName).get(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get field value: " + fieldName, e);
        }
    }

    private void setInternalState(Object object, String fieldName, Object value) {
        try {
            var field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field value: " + fieldName, e);
        }
    }

    private void setupProjectMock(String buildName) {
        Build projectBuild = new Build();
        projectBuild.setFinalName(buildName);
        projectBuild.setDirectory("target/");
        projectBuild.setSourceDirectory("src/");
        projectBuild.setOutputDirectory("classes/");

        when(project.getBasedir()).thenReturn(new File(getBasedir().toURI()));
        when(project.getBuild()).thenReturn(projectBuild);
    }

    private void assertDefaultConfiguration(MonkeyMojo mojo) {
        assertFalse(getFieldValue(mojo, "parsedSkip"), "Monkey skip parameter should be false");
        assertEquals(Integer.valueOf(1000), getFieldValue(mojo, "parsedEventCount"),
                "Monkey eventCount parameter should be 1000");

        // Verify null values
        assertNull(getFieldValue(mojo, "parsedSeed"),
                "Monkey seed should be null");
        assertNull(getFieldValue(mojo, "parsedThrottle"),
                "Monkey throttle should be null");
        assertNull(getFieldValue(mojo, "parsedPercentTouch"),
                "Monkey percentTouch should be null");
        assertNull(getFieldValue(mojo, "parsedPercentMotion"),
                "Monkey percentMotion should be null");
        assertNull(getFieldValue(mojo, "parsedPercentTrackball"),
                "Monkey percentTrackball should be null");
        assertNull(getFieldValue(mojo, "parsedPercentNav"),
                "Monkey percentNav should be null");
        assertNull(getFieldValue(mojo, "parsedPercentMajorNav"),
                "Monkey percentMajorNav should be null");
        assertNull(getFieldValue(mojo, "parsedPercentSyskeys"),
                "Monkey percentSyskeys should be null");
        assertNull(getFieldValue(mojo, "parsedPercentAppswitch"),
                "Monkey percentAppswitch should be null");
        assertNull(getFieldValue(mojo, "parsedPercentAnyevent"),
                "Monkey percentAnyevent should be null");
        assertNull(getFieldValue(mojo, "parsedPackages"),
                "Monkey packages should be null");
        assertNull(getFieldValue(mojo, "parsedCategories"),
                "Monkey categories should be null");

        // Verify boolean defaults
        assertFalse(getFieldValue(mojo, "parsedDebugNoEvents"),
                "Monkey debugNoEvents should be false");
        assertFalse(getFieldValue(mojo, "parsedHprof"),
                "Monkey hprof should be false");
        assertFalse(getFieldValue(mojo, "parsedIgnoreCrashes"),
                "Monkey ignoreCrashes should be false");
        assertFalse(getFieldValue(mojo, "parsedIgnoreTimeouts"),
                "Monkey ignoreTimeouts should be false");
        assertFalse(getFieldValue(mojo, "parsedIgnoreSecurityExceptions"),
                "Monkey ignoreSecurityExceptions should be false");
        assertFalse(getFieldValue(mojo, "parsedKillProcessAfterError"),
                "Monkey killProcessAfterError should be false");
        assertFalse(getFieldValue(mojo, "parsedMonitorNativeCrashes"),
                "Monkey monitorNativeCrashes should be false");
        assertFalse(getFieldValue(mojo, "parsedCreateReport"),
                "Monkey createReport should be false");
    }

    private void assertCustomConfiguration(MonkeyMojo mojo) {
        assertFalse(getFieldValue(mojo, "parsedSkip"), "Monkey skip parameter should be false");
        assertEquals(Integer.valueOf(5000), getFieldValue(mojo, "parsedEventCount"),
                "Monkey eventCount parameter should be 5000");
        assertEquals(Long.valueOf(123456), getFieldValue(mojo, "parsedSeed"),
                "Monkey seed should be 123456");
        assertEquals(Long.valueOf(10), getFieldValue(mojo, "parsedThrottle"),
                "Monkey throttle should be 10");

        // Verify percentage values
        assertEquals(Integer.valueOf(10), getFieldValue(mojo, "parsedPercentTouch"),
                "Monkey percentTouch should be 10");
        assertEquals(Integer.valueOf(10), getFieldValue(mojo, "parsedPercentMotion"),
                "Monkey percentMotion should be 10");
        assertEquals(Integer.valueOf(10), getFieldValue(mojo, "parsedPercentTrackball"),
                "Monkey percentTrackball should be 10");
        assertEquals(Integer.valueOf(10), getFieldValue(mojo, "parsedPercentNav"),
                "Monkey percentNav should be 10");
        assertEquals(Integer.valueOf(10), getFieldValue(mojo, "parsedPercentMajorNav"),
                "Monkey percentMajorNav should be 10");
        assertEquals(Integer.valueOf(10), getFieldValue(mojo, "parsedPercentSyskeys"),
                "Monkey percentSyskeys should be 10");
        assertEquals(Integer.valueOf(10), getFieldValue(mojo, "parsedPercentAppswitch"),
                "Monkey percentAppswitch should be 10");
        assertEquals(Integer.valueOf(10), getFieldValue(mojo, "parsedPercentAnyevent"),
                "Monkey percentAnyevent should be 10");

        // Verify arrays
        String[] expectedPackages = {"com.foo", "com.bar"};
        assertArrayEquals(expectedPackages, getFieldValue(mojo, "parsedPackages"),
                "Monkey packages should be [com.foo,com.bar]");
        String[] expectedCategories = {"foo", "bar"};
        assertArrayEquals(expectedCategories, getFieldValue(mojo, "parsedCategories"),
                "Monkey categories should be [foo,bar]");

        // Verify boolean values
        assertTrue(getFieldValue(mojo, "parsedDebugNoEvents"),
                "Monkey debugNoEvents should be true");
        assertTrue(getFieldValue(mojo, "parsedHprof"),
                "Monkey hprof should be true");
        assertTrue(getFieldValue(mojo, "parsedIgnoreCrashes"),
                "Monkey ignoreCrashes should be true");
        assertTrue(getFieldValue(mojo, "parsedIgnoreTimeouts"),
                "Monkey ignoreTimeouts should be true");
        assertTrue(getFieldValue(mojo, "parsedIgnoreSecurityExceptions"),
                "Monkey ignoreSecurityExceptions should be true");
        assertTrue(getFieldValue(mojo, "parsedKillProcessAfterError"),
                "Monkey killProcessAfterError should be true");
        assertTrue(getFieldValue(mojo, "parsedMonitorNativeCrashes"),
                "Monkey monitorNativeCrashes should be true");
        assertTrue(getFieldValue(mojo, "parsedCreateReport"),
                "Monkey createReport should be true");
    }
}
