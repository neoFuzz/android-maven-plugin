package com.github.cardforge.standalonemojos;

import com.github.cardforge.AbstractAndroidMojoTestCase;
import com.github.cardforge.maven.plugins.android.config.ConfigHandler;
import com.github.cardforge.maven.plugins.android.standalonemojos.MonkeyMojo;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.Ignore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.Arrays;

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
        assertTrue("Monkey skip parameter should be true", automatorSkip);
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

        when(project.getBasedir()).thenReturn(new File(getBasedir()));
        when(project.getBuild()).thenReturn(projectBuild);
    }

    private void assertDefaultConfiguration(MonkeyMojo mojo) {
        assertFalse("Monkey skip parameter should be false", getFieldValue(mojo, "parsedSkip"));
        assertEquals("Monkey eventCount parameter should be 1000",
                Integer.valueOf(1000), getFieldValue(mojo, "parsedEventCount"));

        // Verify null values
        Assertions.assertNull(getFieldValue(mojo, "parsedSeed"),"Monkey seed should be null");
        assertNull("Monkey throttle should be null", getFieldValue(mojo, "parsedThrottle"));
        assertNull("Monkey percentTouch should be null", getFieldValue(mojo, "parsedPercentTouch"));
        assertNull("Monkey percentMotion should be null", getFieldValue(mojo, "parsedPercentMotion"));
        assertNull("Monkey percentTrackball should be null", getFieldValue(mojo, "parsedPercentTrackball"));
        assertNull("Monkey percentNav should be null", getFieldValue(mojo, "parsedPercentNav"));
        assertNull("Monkey percentMajorNav should be null", getFieldValue(mojo, "parsedPercentMajorNav"));
        assertNull("Monkey percentSyskeys should be null", getFieldValue(mojo, "parsedPercentSyskeys"));
        assertNull("Monkey percentAppswitch should be null", getFieldValue(mojo, "parsedPercentAppswitch"));
        assertNull("Monkey percentAnyevent should be null", getFieldValue(mojo, "parsedPercentAnyevent"));
        assertNull("Monkey packages should be null", getFieldValue(mojo, "parsedPackages"));
        assertNull("Monkey categories should be null", getFieldValue(mojo, "parsedCategories"));

        // Verify boolean defaults
        assertFalse("Monkey debugNoEvents should be false", getFieldValue(mojo, "parsedDebugNoEvents"));
        assertFalse("Monkey hprof should be false", getFieldValue(mojo, "parsedHprof"));
        assertFalse("Monkey ignoreCrashes should be false", getFieldValue(mojo, "parsedIgnoreCrashes"));
        assertFalse("Monkey ignoreTimeouts should be false", getFieldValue(mojo, "parsedIgnoreTimeouts"));
        assertFalse("Monkey ignoreSecurityExceptions should be false", getFieldValue(mojo, "parsedIgnoreSecurityExceptions"));
        assertFalse("Monkey killProcessAfterError should be false", getFieldValue(mojo, "parsedKillProcessAfterError"));
        assertFalse("Monkey monitorNativeCrashes should be false", getFieldValue(mojo, "parsedMonitorNativeCrashes"));
        assertFalse("Monkey createReport should be false", getFieldValue(mojo, "parsedCreateReport"));
    }

    private void assertCustomConfiguration(MonkeyMojo mojo) {
        assertFalse("Monkey skip parameter should be false", getFieldValue(mojo, "parsedSkip"));
        assertEquals("Monkey eventCount parameter should be 5000",
                Integer.valueOf(5000), getFieldValue(mojo, "parsedEventCount"));
        assertEquals("Monkey seed should be 123456",
                Long.valueOf(123456), getFieldValue(mojo, "parsedSeed"));
        assertEquals("Monkey throttle should be 10",
                Long.valueOf(10), getFieldValue(mojo, "parsedThrottle"));

        // Verify percentage values
        assertEquals("Monkey percentTouch should be 10",
                Integer.valueOf(10), getFieldValue(mojo, "parsedPercentTouch"));
        assertEquals("Monkey percentMotion should be 10",
                Integer.valueOf(10), getFieldValue(mojo, "parsedPercentMotion"));
        assertEquals("Monkey percentTrackball should be 10",
                Integer.valueOf(10), getFieldValue(mojo, "parsedPercentTrackball"));
        assertEquals("Monkey percentNav should be 10",
                Integer.valueOf(10), getFieldValue(mojo, "parsedPercentNav"));
        assertEquals("Monkey percentMajorNav should be 10",
                Integer.valueOf(10), getFieldValue(mojo, "parsedPercentMajorNav"));
        assertEquals("Monkey percentSyskeys should be 10",
                Integer.valueOf(10), getFieldValue(mojo, "parsedPercentSyskeys"));
        assertEquals("Monkey percentAppswitch should be 10",
                Integer.valueOf(10), getFieldValue(mojo, "parsedPercentAppswitch"));
        assertEquals("Monkey percentAnyevent should be 10",
                Integer.valueOf(10), getFieldValue(mojo, "parsedPercentAnyevent"));

        // Verify arrays
        String[] expectedPackages = {"com.foo", "com.bar"};
        assertTrue("Monkey packages should be [com.foo,com.bar]",
                Arrays.equals(expectedPackages, getFieldValue(mojo, "parsedPackages")));
        String[] expectedCategories = {"foo", "bar"};
        assertTrue("Monkey categories should be [foo,bar]",
                Arrays.equals(expectedCategories, getFieldValue(mojo, "parsedCategories")));

        // Verify boolean values
        assertTrue("Monkey debugNoEvents should be true", getFieldValue(mojo, "parsedDebugNoEvents"));
        assertTrue("Monkey hprof should be true", getFieldValue(mojo, "parsedHprof"));
        assertTrue("Monkey ignoreCrashes should be true", getFieldValue(mojo, "parsedIgnoreCrashes"));
        assertTrue("Monkey ignoreTimeouts should be true", getFieldValue(mojo, "parsedIgnoreTimeouts"));
        assertTrue("Monkey ignoreSecurityExceptions should be true", getFieldValue(mojo, "parsedIgnoreSecurityExceptions"));
        assertTrue("Monkey killProcessAfterError should be true", getFieldValue(mojo, "parsedKillProcessAfterError"));
        assertTrue("Monkey monitorNativeCrashes should be true", getFieldValue(mojo, "parsedMonitorNativeCrashes"));
        assertTrue("Monkey createReport should be true", getFieldValue(mojo, "parsedCreateReport"));
    }
}
