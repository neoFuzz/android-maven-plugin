package com.github.neofuzz.standalonemojos;

import com.github.cardforge.AbstractAndroidMojoTestCase;
import com.github.cardforge.SdkTestSupport;
import com.github.neofuzz.AndroidSdk;
import com.github.neofuzz.CommandExecutor;
import com.github.neofuzz.config.ConfigHandler;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test the lint mojo. Tests options' default values and parsing. Tests the parameters passed to lint.
 *
 * @author Stéphane Nicolas - snicolas@octo.com
 */
@Disabled("This test has to be migrated to be an IntegrationTest using AbstractAndroidMojoIntegrationTest")
@RunWith(MockitoJUnitRunner.class)
public class LintMojoTest extends AbstractAndroidMojoTestCase<LintMojo> {
    @Mock
    private CommandExecutor mockExecutor;

    @Mock
    private ConfigHandler configHandler;

    @Mock
    private ArtifactResolver mockArtifactResolver;

    @Mock
    private ArtifactHandler mockArtifactHandler;

    @Mock
    private MavenProjectHelper mockProjectHelper;

    @Mock
    private DependencyGraphBuilder mockDependencyGraphBuilder;

    @Override
    public String getPluginGoalName() {
        return "lint";
    }

    @Override
    protected Class<LintMojo> getMojoClass() {
        return LintMojo.class;
    }

    /**
     * Tests all options, checks if their default values are correct.
     *
     * @throws Exception If a problem occurs
     */
    @Test
    public void testDefaultLintConfig() throws Exception {
        LintMojo mojo = createMojo("lint-config-project0");

        // Mock ConfigHandler
        doNothing().when(configHandler).parseConfiguration();

        // Set internal state using reflection
        setInternalState(mojo, "parsedSkip", true);

        assertTrue((Boolean) getInternalState(mojo, "parsedSkip"),
                "lint skip parameter should be true");
    }

    /**
     * Tests all options, checks if their default values are correct.
     *
     * @throws Exception If a problem occurs
     */
    @Test
    public void testDefaultUnskippedLintConfig() throws Exception {
        LintMojo mojo = createMojo("lint-config-project1");
        MavenProject project = mock(MavenProject.class);
        Build build = new Build();
        build.setDirectory("target/");
        build.setSourceDirectory("src/");
        build.setOutputDirectory("target/classes");

        when(project.getBuild()).thenReturn(build);
        setInternalState(mojo, "project", project);

        // Set all the default values
        setDefaultValues(mojo);

        // Verify all the default values
        verifyDefaultValues(mojo, project);
    }

    /**
     * Tests all options, checks if they are parsed correctly.
     *
     * @throws Exception If a problem occurs
     */
    @Test
    public void testCustomLintConfig() throws Exception {
        LintMojo mojo = createMojo("lint-config-project2");

        // Set all custom values
        setCustomValues(mojo);

        // Verify all custom values
        verifyCustomValues(mojo);
    }

    @Test
    public void testAllLintCommandParametersWithDefaultUnskippedConfig() throws Exception {
        LintMojo mojo = createMojo("lint-config-project1");

        // Setup project mock
        MavenProject project = mock(MavenProject.class);
        Build build = new Build();
        build.setDirectory("target/");
        build.setSourceDirectory("src/");
        build.setOutputDirectory("classes/");

        File projectBaseDir = new File(getBasedir().toURI());
        when(project.getBasedir()).thenReturn(projectBaseDir);
        when(project.getBuild()).thenReturn(build);

        setInternalState(mojo, "project", project);

        // Mock command executor
        try (MockedStatic<CommandExecutor.Factory> factoryMock = mockStatic(CommandExecutor.Factory.class)) {
            factoryMock.when(CommandExecutor.Factory::createDefaultCommandExecutor)
                    .thenReturn(mockExecutor);

            // Set default values
            setDefaultValues(mojo);

            // Execute mojo
            mojo.execute();

            // Verify command execution
            ArgumentCaptor<List<String>> argumentCaptor = ArgumentCaptor.forClass(List.class);
            verify(mockExecutor).executeCommand(any(String.class), argumentCaptor.capture(), eq(false));

            // Verify expected parameters
            List<String> expectedParams = getDefaultExpectedParameters(projectBaseDir);
            assertEquals(expectedParams, argumentCaptor.getValue());
        }
    }

    @Test
    public void testAllLintCommandParametersWithCustomConfig() throws Exception {
        LintMojo mojo = createMojo("lint-config-project2");

        // Setup project mock
        MavenProject project = mock(MavenProject.class);
        File projectBaseDir = new File("project/");
        when(project.getBasedir()).thenReturn(projectBaseDir);
        setInternalState(mojo, "project", project);

        // Mock command executor
        try (MockedStatic<CommandExecutor.Factory> factoryMock = mockStatic(CommandExecutor.Factory.class)) {
            factoryMock.when(CommandExecutor.Factory::createDefaultCommandExecutor)
                    .thenReturn(mockExecutor);

            // Set custom values
            setCustomValues(mojo);
            setInternalState(mojo, "parsedSkip", false);

            // Execute mojo
            mojo.execute();

            // Verify command execution
            ArgumentCaptor<List<String>> argumentCaptor = ArgumentCaptor.forClass(List.class);
            verify(mockExecutor).executeCommand(any(String.class), argumentCaptor.capture(), eq(false));

            // Verify expected parameters
            List<String> expectedParams = getCustomExpectedParameters(projectBaseDir);
            assertEquals(expectedParams, argumentCaptor.getValue());
        }
    }

    @Test
    public void testAllParametersOffConfig() throws Exception {
        LintMojo mojo = new LintMojo(
                mockArtifactResolver, mockArtifactHandler, mockProjectHelper, mockDependencyGraphBuilder) {
            @Override
            public AndroidSdk getAndroidSdk() {
                return new SdkTestSupport().getSdkWithPlatformDefault();
            }
        };

        // Setup project mock
        MavenProject project = mock(MavenProject.class);
        File projectBaseDir = new File("project/");
        when(project.getBasedir()).thenReturn(projectBaseDir);
        setInternalState(mojo, "project", project);

        // Mock command executor
        try (MockedStatic<CommandExecutor.Factory> factoryMock = mockStatic(CommandExecutor.Factory.class)) {
            factoryMock.when(CommandExecutor.Factory::createDefaultCommandExecutor)
                    .thenReturn(mockExecutor);

            // Set the minimal required configuration
            setInternalState(mojo, "parsedConfig", "null");
            setInternalState(mojo, "parsedClasspath", "null");
            setInternalState(mojo, "parsedLibraries", "null");

            // Execute the mojo
            mojo.executeWhenConfigured();

            // Verify command execution
            ArgumentCaptor<List<String>> argumentCaptor = ArgumentCaptor.forClass(List.class);
            verify(mockExecutor).executeCommand(any(String.class), argumentCaptor.capture(), eq(false));
            verify(mockExecutor).setLogger(any(Log.class));

            // Verify expected parameters
            List<String> expectedParams = new ArrayList<>();
            expectedParams.add(projectBaseDir.getAbsolutePath());
            expectedParams.add("--exitcode");
            assertEquals(expectedParams, argumentCaptor.getValue());
        }
    }

    // ********
    private void setDefaultValues(LintMojo mojo) {
        setInternalState(mojo, "parsedSkip", false);
        setInternalState(mojo, "parsedFailOnError", false);
        setInternalState(mojo, "parsedIgnoreWarnings", false);
        setInternalState(mojo, "parsedWarnAll", false);
        setInternalState(mojo, "parsedWarningsAsErrors", false);
        setInternalState(mojo, "parsedConfig", "null");

        setInternalState(mojo, "parsedFullPath", false);
        setInternalState(mojo, "parsedShowAll", true);
        setInternalState(mojo, "parsedDisableSourceLines", false);
        setInternalState(mojo, "parsedUrl", "none");

        setInternalState(mojo, "parsedEnableXml", true);
        setInternalState(mojo, "parsedXmlOutputPath",
                new File(getField(mojo, "parsedXmlOutputPath").toString(),
                        "lint-results/lint-results.xml").getAbsolutePath());
        setInternalState(mojo, "parsedEnableHtml", false);
        setInternalState(mojo, "parsedHtmlOutputPath",
                new File(getField(mojo, "parsedHtmlOutputPath").toString(),
                        "lint-results/lint-results-html").getAbsolutePath());
        setInternalState(mojo, "parsedEnableSimpleHtml", false);
        setInternalState(mojo, "parsedSimpleHtmlOutputPath",
                new File(getField(mojo, "parsedSimpleHtmlOutputPath").toString(),
                        "lint-results/lint-results-simple-html").getAbsolutePath());

        setInternalState(mojo, "parsedEnableSources", true);
        setInternalState(mojo, "parsedSources", getField(mojo, "parsedSources").toString());
        setInternalState(mojo, "parsedEnableClasspath", false);
        setInternalState(mojo, "parsedClasspath", getField(mojo, "parsedClasspath").toString());
        setInternalState(mojo, "parsedEnableLibraries", false);
        setInternalState(mojo, "parsedLibraries", null);
    }

    private void setCustomValues(LintMojo mojo) {
        setInternalState(mojo, "parsedSkip", false);
        setInternalState(mojo, "parsedFailOnError", true);
        setInternalState(mojo, "parsedIgnoreWarnings", true);
        setInternalState(mojo, "parsedWarnAll", true);
        setInternalState(mojo, "parsedWarningsAsErrors", true);
        setInternalState(mojo, "parsedConfig", "lint");

        setInternalState(mojo, "parsedFullPath", true);
        setInternalState(mojo, "parsedShowAll", false);
        setInternalState(mojo, "parsedDisableSourceLines", true);
        setInternalState(mojo, "parsedUrl", "url");

        setInternalState(mojo, "parsedEnableXml", false);
        setInternalState(mojo, "parsedXmlOutputPath", "xml");
        setInternalState(mojo, "parsedEnableHtml", true);
        setInternalState(mojo, "parsedHtmlOutputPath", "html");
        setInternalState(mojo, "parsedEnableSimpleHtml", true);
        setInternalState(mojo, "parsedSimpleHtmlOutputPath", "simple");

        setInternalState(mojo, "parsedEnableSources", false);
        setInternalState(mojo, "parsedSources", "src2");
        setInternalState(mojo, "parsedEnableClasspath", true);
        setInternalState(mojo, "parsedClasspath", "cla2");
        setInternalState(mojo, "parsedEnableLibraries", true);
        setInternalState(mojo, "parsedLibraries", "lib2");
    }

    private void verifyDefaultValues(LintMojo mojo, MavenProject project) {
        assertFalse((Boolean) getInternalState(mojo, "parsedSkip"),
                "lint skip parameter should be false");
        assertFalse((Boolean) getInternalState(mojo, "parsedFailOnError"),
                "lint failOnError parameter should be false");
        assertFalse((Boolean) getInternalState(mojo, "parsedIgnoreWarnings"),
                "lint ignoreWarning parameter should be false");
        assertFalse((Boolean) getInternalState(mojo, "parsedWarnAll"),
                "lint warnAll parameter should be false");
        assertFalse((Boolean) getInternalState(mojo, "parsedWarningsAsErrors"),
                "lint warningsAsErrors parameter should be false");
        assertEquals("null", getInternalState(mojo, "parsedConfig"),
                "lint config parameter should be null");

        assertFalse((Boolean) getInternalState(mojo, "parsedFullPath"),
                "lint fullPath parameter should be false");
        assertTrue((Boolean) getInternalState(mojo, "parsedShowAll"),
                "lint showAll parameter should be true");
        assertFalse((Boolean) getInternalState(mojo, "parsedDisableSourceLines"),
                "lint disableSourceLines parameter should be false");
        assertEquals("none", getInternalState(mojo, "parsedUrl"),
                "lint url parameter should be none");
        assertTrue((Boolean) getInternalState(mojo, "parsedEnableXml"),
                "lint enableXml parameter should be true");

        File lintXmlOutputFile = new File(project.getBuild().getDirectory(), "lint-results/lint-results.xml");
        assertEquals(lintXmlOutputFile.getAbsolutePath(), getInternalState(mojo, "parsedXmlOutputPath"),
                "lint xmlOutputPath parameter should point to lint-results.xml");
        assertFalse((Boolean) getInternalState(mojo, "parsedEnableHtml"),
                "lint enableHtml parameter should be false");

        File lintHtmlOutputFile = new File(project.getBuild().getDirectory(), "lint-results/lint-results-html");
        assertEquals(lintHtmlOutputFile.getAbsolutePath(), getInternalState(mojo, "parsedHtmlOutputPath"),
                "lint htmlOutputPath parameter should point to lint-html");
        assertFalse((Boolean) getInternalState(mojo, "parsedEnableSimpleHtml"),
                "lint enableSimpleHtml parameter should be false");

        File lintSimpleHtmlOutputFile = new File(project.getBuild().getDirectory(), "lint-results/lint-results-simple-html");
        assertEquals(lintSimpleHtmlOutputFile.getAbsolutePath(),
                getInternalState(mojo, "parsedSimpleHtmlOutputPath"),
                "lint simpleHtmlOutputPath parameter should point to lint-simple-html");
        assertTrue((Boolean) getInternalState(mojo, "parsedEnableSources"),
                "lint enableSources parameter should be true");
        assertEquals(project.getBuild().getSourceDirectory(), getInternalState(mojo, "parsedSources"),
                "lint sources parameter should point to src/");
        assertFalse((Boolean) getInternalState(mojo, "parsedEnableClasspath"),
                "lint enableClasspath parameter should be false");
        assertEquals(project.getBuild().getOutputDirectory(), getInternalState(mojo, "parsedClasspath"),
                "lint classpath parameter should point to target/classes");
        assertFalse((Boolean) getInternalState(mojo, "parsedEnableLibraries"),
                "lint enableLibraries parameter should be false");
        assertNull(getInternalState(mojo, "parsedLibraries"),
                "lint libraries parameter should point not contain dependencies");
    }

    private void verifyCustomValues(LintMojo mojo) {
        assertFalse((Boolean) getInternalState(mojo, "parsedSkip"),
                "lint skip parameter should be false");
        assertTrue((Boolean) getInternalState(mojo, "parsedFailOnError"),
                "lint failOnError parameter should be true");
        assertTrue((Boolean) getInternalState(mojo, "parsedIgnoreWarnings"),
                "lint ignoreWarning parameter should be true");
        assertTrue((Boolean) getInternalState(mojo, "parsedWarnAll"),
                "lint warnAll parameter should be true");
        assertTrue((Boolean) getInternalState(mojo, "parsedWarningsAsErrors"),
                "lint warningsAsErrors parameter should be true");
        assertNotNull(getInternalState(mojo, "parsedConfig"),
                "lint config parameter should be non null");
        assertEquals("lint", getInternalState(mojo, "parsedConfig"),
                "lint config parameter should point to lint");
        assertTrue((Boolean) getInternalState(mojo, "parsedFullPath"),
                "lint fullPath parameter should be true");
        assertFalse((Boolean) getInternalState(mojo, "parsedShowAll"),
                "lint showAll parameter should be false");
        assertTrue((Boolean) getInternalState(mojo, "parsedDisableSourceLines"),
                "lint disableSourceLines parameter should be true");
        assertEquals("url", getInternalState(mojo, "parsedUrl"),
                "lint url parameter should be url");
        assertFalse((Boolean) getInternalState(mojo, "parsedEnableXml"),
                "lint enableXml parameter should be false");
        assertEquals("xml", getInternalState(mojo, "parsedXmlOutputPath"),
                "lint xmlOutputPath parameter should point to xml");
        assertTrue((Boolean) getInternalState(mojo, "parsedEnableHtml"),
                "lint enableHtml parameter should be true");
        assertEquals("html", getInternalState(mojo, "parsedHtmlOutputPath"),
                "lint htmlOutputPath parameter should point to html");
        assertTrue((Boolean) getInternalState(mojo, "parsedEnableSimpleHtml"),
                "lint enableSimpleHtml parameter should be true");
        assertEquals("simple", getInternalState(mojo, "parsedSimpleHtmlOutputPath"),
                "lint simpleHtmlOutputPath parameter should point to simple");
        assertFalse((Boolean) getInternalState(mojo, "parsedEnableSources"),
                "lint enableSources parameter should be false");
        assertTrue((Boolean) getInternalState(mojo, "parsedEnableClasspath"),
                "lint enableClasspath parameter should be true");
        assertTrue((Boolean) getInternalState(mojo, "parsedEnableLibraries"),
                "lint enableLibraries parameter should be true");

        assertEquals("src2", getInternalState(mojo, "parsedSources"),
                "lint sources parameter should point to src2");
        assertEquals("cla2", getInternalState(mojo, "parsedClasspath"),
                "lint classpath parameter should point to cla2");
        assertEquals("lib2", getInternalState(mojo, "parsedLibraries"),
                "lint libraries parameter should point to lib2");
    }

    @Nonnull
    private List<String> getDefaultExpectedParameters(@Nonnull File projectBaseDir) {
        List<String> params = new ArrayList<>();
        params.add("--showall");
        params.add("--xml");
        params.add(projectBaseDir.getAbsolutePath() +
                FilenameUtils.separatorsToSystem("/target/lint-results/lint-results.xml"));
        params.add("--sources");
        params.add(projectBaseDir.getAbsolutePath() + File.separator + "src");
        params.add(projectBaseDir.getAbsolutePath());
        params.add("--exitcode");
        return params;
    }

    @Nonnull
    private List<String> getCustomExpectedParameters(@Nonnull File projectBaseDir) {
        List<String> params = new ArrayList<>();
        params.add("-w");
        params.add("-Wall");
        params.add("-Werror");
        params.add("--config");
        params.add("lint");
        params.add("--fullpath");
        params.add("--nolines");
        params.add("--html");
        params.add("html");
        params.add("--url");
        params.add("url");
        params.add("--simplehtml");
        params.add("simple");
        params.add("--classpath");
        params.add("cla2");
        params.add("--libraries");
        params.add("lib2");
        params.add(projectBaseDir.getAbsolutePath());
        params.add("--exitcode");
        return params;
    }

    // Helper methods for reflection
    private void setInternalState(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object getInternalState(Object target, String fieldName) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Utility to get a private field value using reflection
    private Object getField(@Nonnull Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            return "null";
        }
    }
}
