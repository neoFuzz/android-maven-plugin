package com.github.cardforge.standalonemojos;

import com.github.cardforge.AbstractAndroidMojoTestCase;
import com.github.cardforge.maven.plugins.android.CommandExecutor;
import com.github.cardforge.maven.plugins.android.common.AndroidExtension;
import com.github.cardforge.maven.plugins.android.config.ConfigHandler;
import com.github.cardforge.maven.plugins.android.standalonemojos.ZipalignMojo;
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
        assertTrue("zipalign 'skip' parameter should be true", skip);

        Boolean verbose = getPrivateField(mojo, "parsedVerbose");
        assertFalse("zipalign 'verbose' parameter should be false", verbose);

        MavenProject project = getPrivateField(mojo, "project");

        String inputApk = getPrivateField(mojo, "parsedInputApk");
        File inputApkFile = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".apk");
        assertEquals("zipalign 'inputApk' parameter should be equal", inputApkFile.getAbsolutePath(), inputApk);

        String outputApk = getPrivateField(mojo, "parsedOutputApk");
        File outputApkFile = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + "-aligned.apk");
        assertEquals("zipalign 'outputApk' parameter should be equal", outputApkFile.getAbsolutePath(), outputApk);
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
        assertFalse("zipalign 'skip' parameter should be false", skip);

        Boolean verbose = getPrivateField(mojo, "parsedVerbose");
        assertTrue("zipalign 'verbose' parameter should be true", verbose);

        String inputApk = getPrivateField(mojo, "parsedInputApk");
        assertEquals("zipalign 'inputApk' parameter should be equal", "app.apk", inputApk);

        String outputApk = getPrivateField(mojo, "parsedOutputApk");
        assertEquals("zipalign 'outputApk' parameter should be equal", "app-updated.apk", outputApk);
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
            assertEquals("Zipalign arguments aren't as expected", expectedParameters, parameters);

            // Verify attachArtifact was called with the correct file
            File expectedFile = new File("app-updated.apk");
            Mockito.verify(projectHelper).attachArtifact(
                    Mockito.eq(project),
                    Mockito.eq(AndroidExtension.APK),
                    Mockito.eq("aligned"),
                    Mockito.eq(expectedFile)
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
            assertEquals("Zipalign arguments aren't as expected", expectedParameters, parameters);

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

