package com.github.cardforge.standalonemojos;

import com.github.cardforge.AbstractAndroidMojoTestCase;
import com.github.cardforge.maven.plugins.android.config.ConfigHandler;
import com.github.cardforge.maven.plugins.android.standalonemojos.UIAutomatorMojo;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Test the UIAutomator mojo. Tests options' default values and parsing. We do not test the command line that is passed
 * to the adb bridge, it should be possible to mock it though.
 *
 * @author Stéphane Nicolas - snicolas@octo.com
 */
@Disabled("This test has to be migrated to be an IntegrationTest using AbstractAndroidMojoIntegrationTest")
@RunWith(MockitoJUnitRunner.class)
public class UIAutomatorMojoTest extends AbstractAndroidMojoTestCase<UIAutomatorMojo> {
    @Override
    public String getPluginGoalName() {
        return "uiautomator";
    }

    /**
     * Tests all options, checks if their default values are correct.
     *
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void testDefaultUIAutomatorConfig() throws Exception {
        // given
        UIAutomatorMojo mojo = createMojo("ui-automator-config-project0");
        ConfigHandler configHandler = new ConfigHandler(mojo, this.session, this.execution);
        configHandler.parseConfiguration();

        // when
        Boolean automatorSkip = getPrivateField(mojo, "parsedSkip");

        // then
        assertTrue("UIAutomator skip parameter should be true", automatorSkip);
    }

    /**
     * Tests all options, checks if their default values are correct.
     *
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void testDefaultUnskippedUIAutomatorConfig() throws Exception {
        // given
        UIAutomatorMojo mojo = createMojo("ui-automator-config-project1");

        // Mock MavenProject
        MavenProject project = Mockito.mock(MavenProject.class);
        setPrivateField(mojo, "project", project);

        File projectBaseDir = new File(getBasedir());
        Build projectBuild = new Build();
        String buildName = "ui-automator-config-project1-15.4.3.1011";
        projectBuild.setFinalName(buildName);
        projectBuild.setDirectory("target/");
        projectBuild.setSourceDirectory("src/");
        projectBuild.setOutputDirectory("classes/");

        Mockito.when(project.getBasedir()).thenReturn(projectBaseDir);
        Mockito.when(project.getBuild()).thenReturn(projectBuild);

        // when
        ConfigHandler configHandler = new ConfigHandler(mojo, this.session, this.execution);
        configHandler.parseConfiguration();

        // then
        Boolean automatorSkip = getPrivateField(mojo, "parsedSkip");
        Boolean automatorDebug = getPrivateField(mojo, "parsedDebug");
        String automatorJarFile = getPrivateField(mojo, "parsedJarFile");
        String[] automatorTestClassOrMethods = getPrivateField(mojo, "parsedTestClassOrMethods");
        Boolean automatorTakeScreenshotOnFailure = getPrivateField(mojo, "parsedTakeScreenshotOnFailure");
        String automatorScreenshotsPathOnDevice = getPrivateField(mojo, "parsedScreenshotsPathOnDevice");
        Boolean automatorCreateReport = getPrivateField(mojo, "parsedCreateReport");
        String automatorReportSuffix = getPrivateField(mojo, "parsedReportSuffix");
        String automatorPropertiesKeyPrefix = getPrivateField(mojo, "parsedPropertiesKeyPrefix");

        assertFalse("UIAutomator skip parameter should be false", automatorSkip);
        assertFalse("UIAutomator debug parameter should be false", automatorDebug);
        String expectedJarFile = buildName + ".jar";
        assertNotNull("UIAutomator jarFile parameter should not be null", automatorJarFile);
        assertEquals("UIAutomator jarFile parameter should match artifact name", expectedJarFile, automatorJarFile);
        assertNull("UIAutomator testClassOrMethods parameter should be null", automatorTestClassOrMethods);
        assertFalse("UIAutomator takeScreenshotOnFailure parameter should be false", automatorTakeScreenshotOnFailure);
        assertEquals("/sdcard/uiautomator-screenshots/", automatorScreenshotsPathOnDevice);
        assertFalse("UIAutomator createReport parameter should be false", automatorCreateReport);
        assertNull("UIAutomator reportSuffix parameter should be null", automatorReportSuffix);
        assertNull("UIAutomator propertiesKeyPrefix parameter should be null", automatorPropertiesKeyPrefix);
    }

    /**
     * Tests all options, checks if they are parsed correctly.
     *
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void testCustomUIAutomatorConfig() throws Exception {
        // given
        UIAutomatorMojo mojo = createMojo("ui-automator-config-project2");

        // Mock MavenProject
        MavenProject project = Mockito.mock(MavenProject.class);
        setPrivateField(mojo, "project", project);

        File projectBaseDir = new File(getBasedir());
        Build projectBuild = new Build();
        String buildName = "ui-automator-config-project1-15.4.3.1011";
        projectBuild.setFinalName(buildName);
        projectBuild.setDirectory("target/");
        projectBuild.setSourceDirectory("src/");
        projectBuild.setOutputDirectory("classes/");

        Mockito.when(project.getBasedir()).thenReturn(projectBaseDir);
        Mockito.when(project.getBuild()).thenReturn(projectBuild);

        // when
        ConfigHandler configHandler = new ConfigHandler(mojo, this.session, this.execution);
        configHandler.parseConfiguration();

        // then
        Boolean automatorSkip = getPrivateField(mojo, "parsedSkip");
        Boolean automatorDebug = getPrivateField(mojo, "parsedDebug");
        String automatorJarFile = getPrivateField(mojo, "parsedJarFile");
        String[] automatorTestClassOrMethods = getPrivateField(mojo, "parsedTestClassOrMethods");
        Boolean automatorTakeScreenshotOnFailure = getPrivateField(mojo, "parsedTakeScreenshotOnFailure");
        String automatorScreenshotsPathOnDevice = getPrivateField(mojo, "parsedScreenshotsPathOnDevice");
        Boolean automatorCreateReport = getPrivateField(mojo, "parsedCreateReport");
        String automatorReportSuffix = getPrivateField(mojo, "parsedReportSuffix");
        String automatorPropertiesKeyPrefix = getPrivateField(mojo, "parsedPropertiesKeyPrefix");

        assertFalse("UIAutomator skip parameter should be false", automatorSkip);
        assertFalse("UIAutomator debug parameter should be false", automatorDebug);
        assertNotNull("UIAutomator jarFile parameter should not be null", automatorJarFile);
        String expectedJarFile = buildName + ".jar";
        assertEquals("UIAutomator jarFile parameter should match artifact name", expectedJarFile, automatorJarFile);

        assertNotNull("UIAutomator testClassOrMethods parameter should not be null", automatorTestClassOrMethods);
        String[] expectedTestClassOrMethods = {"a", "b#c"};
        assertArrayEquals(expectedTestClassOrMethods, automatorTestClassOrMethods);

        assertTrue("UIAutomator takeScreenshotOnFailure parameter should be true", automatorTakeScreenshotOnFailure);
        assertEquals("/mnt/sdcard/screenshots/", automatorScreenshotsPathOnDevice);
        assertTrue("UIAutomator createReport parameter should be true", automatorCreateReport);
        assertEquals("-mySpecialReport", automatorReportSuffix);
        assertEquals("UIA", automatorPropertiesKeyPrefix);
    }

    /**
     * Helper method to get private fields using reflection
     *
     * @param target    Object to get the field from
     * @param fieldName Name of the field to get
     * @param <T>       Type of the field to get
     * @return Value of the field
     * @throws Exception if any error occurs during the field retrieval
     */
    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(@Nonnull Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    /**
     * Helper method to set private fields using reflection
     *
     * @param target    Object to set the field on
     * @param fieldName Name of the field to set
     * @param value     Value to set the field to
     * @throws Exception if any error occurs during the field setting
     */
    private void setPrivateField(@Nonnull Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
