package com.github.cardforge.standalonemojos;

import com.github.cardforge.AbstractAndroidMojoTestCase;
import com.github.neofuzz.CommandExecutor;
import com.github.neofuzz.common.AndroidExtension;
import com.github.neofuzz.config.ConfigHandler;
import com.github.neofuzz.standalonemojos.ZipalignMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Eugen
 */
@Ignore("This test has to be migrated to be an IntegrationTest using AbstractAndroidMojoIntegrationTest")
@RunWith(MockitoJUnitRunner.class)
public class ZipalignMojoTest extends AbstractAndroidMojoTestCase<ZipalignMojo> {
    @Override
    public String getPluginGoalName() {
        return "zipalign";
    }

    @Override
    protected Class<ZipalignMojo> getMojoClass() {
        return ZipalignMojo.class;
    }

    /**
     * Tests all options, checks if their default values are correct.
     *
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void testDefaultConfig() throws Exception {
        ZipalignMojo mojo = createMojo("zipalign-config-project0");
        ConfigHandler configHandler = new ConfigHandler(mojo, this.session, this.execution);
        configHandler.parseConfiguration();

        Boolean skip = getPrivateField(mojo, "parsedSkip");
        assertTrue(skip, "zipalign 'skip' parameter should be true");

        Boolean verbose = getPrivateField(mojo, "parsedVerbose");
        assertFalse(verbose, "zipalign 'verbose' parameter should be false");

        MavenProject project = getPrivateField(mojo, "project");

        String inputApk = getPrivateField(mojo, "parsedInputApk");
        File inputApkFile = new File(project.getBuild().getDirectory(),
                project.getBuild().getFinalName() + ".apk");
        assertEquals(inputApkFile.getAbsolutePath(), inputApk, "zipalign 'inputApk' parameter should be equal");

        String outputApk = getPrivateField(mojo, "parsedOutputApk");
        File outputApkFile = new File(project.getBuild().getDirectory(),
                project.getBuild().getFinalName() + "-aligned.apk");
        assertEquals(outputApkFile.getAbsolutePath(), outputApk,
                "zipalign 'outputApk' parameter should be equal");
    }

    /**
     * Tests all parameters parsing
     * <p>
     * Probably not needed since it is like testing maven itself
     *
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void testConfigParse() throws Exception {
        ZipalignMojo mojo = createMojo("zipalign-config-project1");
        ConfigHandler configHandler = new ConfigHandler(mojo, this.session, this.execution);
        configHandler.parseConfiguration();

        Boolean skip = getPrivateField(mojo, "parsedSkip");
        assertFalse(skip, "zipalign 'skip' parameter should be false");

        Boolean verbose = getPrivateField(mojo, "parsedVerbose");
        assertTrue(verbose, "zipalign 'verbose' parameter should be true");

        String inputApk = getPrivateField(mojo, "parsedInputApk");
        assertEquals("app.apk", inputApk, "zipalign 'inputApk' parameter should be equal");

        String outputApk = getPrivateField(mojo, "parsedOutputApk");
        assertEquals("app-updated.apk", outputApk, "zipalign 'outputApk' parameter should be equal");
    }

    /**
     * Tests run of zipalign with correct parameters as well adding aligned file to artifacts
     *
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void testDefaultRun() throws Exception {
        ZipalignMojo mojo = createMojo("zipalign-config-project3");

        MavenProject project = getPrivateField(mojo, "project");
        project.setPackaging(AndroidExtension.APK);

        MavenProjectHelper projectHelper = Mockito.mock(MavenProjectHelper.class);
        setPrivateField(mojo, "projectHelper", projectHelper);

        CommandExecutor mockExecutor = Mockito.mock(CommandExecutor.class);
        setPrivateField(CommandExecutor.Factory.class, "executor", mockExecutor);

        // Capture arguments passed to CommandExecutor
        @SuppressWarnings("unchecked") ArgumentCaptor<List<String>> commandCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.doNothing().when(mockExecutor).setLogger(Mockito.any(Log.class));
        Mockito.doNothing().when(mockExecutor).executeCommand(Mockito.anyString(), commandCaptor.capture());

        // Mock static methods in FileUtils
        try (var mockedFileUtils = Mockito.mockStatic(FileUtils.class)) {
            mockedFileUtils.when(() -> FileUtils.fileExists("app-updated.apk")).thenReturn(true);

            // when
            mojo.execute();

            // then
            Mockito.verify(mockExecutor).executeCommand(Mockito.anyString(), Mockito.anyList());
            Mockito.verify(projectHelper).attachArtifact(
                    Mockito.eq(project),
                    Mockito.eq(AndroidExtension.APK),
                    Mockito.eq("aligned"),
                    Mockito.any(File.class)
            );

            // Verify the arguments passed to zipalign
            List<String> parameters = commandCaptor.getValue();
            List<String> expectedParameters = Arrays.asList("-v", "-f", "4", "app.apk", "app-updated.apk");
            assertEquals(expectedParameters, parameters, "Zipalign arguments aren't as expected");

            // Verify attachArtifact was called with the correct file
            File expectedFile = new File("app-updated.apk");
            Mockito.verify(projectHelper).attachArtifact(
                    (project),
                    (AndroidExtension.APK),
                    ("aligned"),
                    (expectedFile)
            );

            mockedFileUtils.verify(() -> FileUtils.fileExists("app-updated.apk"));
        }
    }

    /**
     * Tests run of zipalign with correct parameters
     *
     * @throws Exception if any error occurs during the test execution
     */
    @Test
    public void testRunWhenInputApkIsSameAsOutput() throws Exception {
        // given
        ZipalignMojo mojo = createMojo("zipalign-config-project2");

        MavenProject project = getPrivateField(mojo, "project");
        project.setPackaging(AndroidExtension.APK);

        MavenProjectHelper projectHelper = Mockito.mock(MavenProjectHelper.class);
        setPrivateField(mojo, "projectHelper", projectHelper);

        CommandExecutor mockExecutor = Mockito.mock(CommandExecutor.class);
        setPrivateField(CommandExecutor.Factory.class, "executor", mockExecutor);

        // Capture arguments passed to CommandExecutor
        @SuppressWarnings("unchecked") ArgumentCaptor<List<String>> commandCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.doNothing().when(mockExecutor).setLogger(Mockito.any(Log.class));
        Mockito.doNothing().when(mockExecutor).executeCommand(Mockito.anyString(), commandCaptor.capture());

        // Mock static methods in FileUtils
        try (var mockedFileUtils = Mockito.mockStatic(FileUtils.class)) {
            mockedFileUtils.when(() -> FileUtils.fileExists("app-aligned-temp.apk")).thenReturn(true);
            mockedFileUtils.when(() -> FileUtils.rename(new File("app-aligned-temp.apk"), new File("app.apk")));

            // when
            mojo.execute();

            // then
            Mockito.verify(mockExecutor).executeCommand(Mockito.anyString(), Mockito.anyList());

            List<String> parameters = commandCaptor.getValue();
            List<String> expectedParameters = Arrays.asList("-v", "-f", "4", "app.apk", "app-aligned-temp.apk");
            assertEquals(expectedParameters, parameters, "Zipalign arguments aren't as expected");

            mockedFileUtils.verify(() -> FileUtils.rename(new File("app-aligned-temp.apk"), new File("app.apk")));
        }
    }

    // Helper method to get private fields using reflection
    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(@Nonnull Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    // Helper method to set private fields using reflection
    private void setPrivateField(@Nonnull Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

