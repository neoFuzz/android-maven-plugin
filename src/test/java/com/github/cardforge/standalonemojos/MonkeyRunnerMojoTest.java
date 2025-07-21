package com.github.cardforge.standalonemojos;

import com.github.cardforge.AbstractAndroidMojoTestCase;
import com.github.neofuzz.CommandExecutor;
import com.github.neofuzz.config.ConfigHandler;
import com.github.neofuzz.configuration.Program;
import com.github.neofuzz.standalonemojos.MonkeyRunnerMojo;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the monkeyrunner mojo. Tests options' default values and parsing. Tests the parameters passed to monkeyrunner.
 *
 * @author Stéphane Nicolas - snicolas@octo.com
 */
@Disabled("This test has to be migrated to be an IntegrationTest using AbstractAndroidMojoIntegrationTest")
@RunWith(MockitoJUnitRunner.class)
public class MonkeyRunnerMojoTest extends AbstractAndroidMojoTestCase<MonkeyRunnerMojo> {
    @Override
    public String getPluginGoalName() {
        return "monkeyrunner";
    }

    @Override
    protected Class<MonkeyRunnerMojo> getMojoClass() {
        return MonkeyRunnerMojo.class;
    }

    /**
     * Tests all options, checks if their default values are correct.
     *
     * @throws Exception if any error occurs
     */
    @Test
    public void testDefaultMonkeyRunnerConfig() throws Exception {
        MonkeyRunnerMojo mojo = createMojo("monkey-runner-config-project0");
        ConfigHandler configHandler = new ConfigHandler(mojo, this.session, this.execution);
        configHandler.parseConfiguration();

        Boolean monkeyrunnerSkip = getPrivateField(mojo, "parsedSkip");

        assertTrue(monkeyrunnerSkip, "monkeyrunner skip parameter should be true");
    }

    /**
     * Tests all options, checks if their default values are correct.
     *
     * @throws Exception if any error occurs
     */
    @Test
    public void testDefaultUnskippedMonkeyRunnerConfig() throws Exception {
        MonkeyRunnerMojo mojo = createMojo("monkey-runner-config-project1");
        ConfigHandler configHandler = new ConfigHandler(mojo, this.session, this.execution);
        configHandler.parseConfiguration();

        Boolean monkeyrunnerSkip = getPrivateField(mojo, "parsedSkip");
        String[] monkeyrunnerPlugins = getPrivateField(mojo, "parsedPlugins");
        List<Program> monkeyrunnerPrograms = getPrivateField(mojo, "parsedPrograms");
        Boolean monkeyrunnerCreateReport = getPrivateField(mojo, "parsedCreateReport");

        assertFalse(monkeyrunnerSkip, "monkeyrunner skip parameter should be false");
        assertNull(monkeyrunnerPlugins, "monkeyrunner plugins parameter should not contain plugins");
        assertNull(monkeyrunnerPrograms, "monkeyrunner programs parameter should not contain programs");
        assertFalse(monkeyrunnerCreateReport, "monkeyrunner monkeyrunnerCreateReport parameter should be false");
    }

    /**
     * Tests all options, checks if they are parsed correctly.
     *
     * @throws Exception if any error occurs
     */
    @Test
    public void testCustomMonkeyRunnerConfig() throws Exception {
        MonkeyRunnerMojo mojo = createMojo("monkey-runner-config-project2");
        ConfigHandler configHandler = new ConfigHandler(mojo, this.session, this.execution);
        configHandler.parseConfiguration();

        Boolean monkeyrunnerSkip = getPrivateField(mojo, "parsedSkip");
        String[] monkeyrunnerPlugins = getPrivateField(mojo, "parsedPlugins");
        List<Program> monkeyrunnerPrograms = getPrivateField(mojo, "parsedPrograms");
        Boolean monkeyrunnerCreateReport = getPrivateField(mojo, "parsedCreateReport");

        assertFalse(monkeyrunnerSkip, "monkeyrunner skip parameter should be false");
        assertNotNull(monkeyrunnerPlugins, "monkeyrunner plugins parameter should not contain plugins");
        String[] expectedPlugins = {"foo"};
        assertArrayEquals(expectedPlugins, monkeyrunnerPlugins);
        assertNotNull(monkeyrunnerPrograms, "monkeyrunner programs parameter should not contain programs");
        List<Program> expectedProgramList = new ArrayList<>();
        expectedProgramList.add(new Program("foo", null));
        expectedProgramList.add(new Program("bar", "qux"));
        assertEquals(expectedProgramList, monkeyrunnerPrograms);
        assertTrue(monkeyrunnerCreateReport, "monkeyrunner monkeyrunnerCreateReport parameter should be false");
    }

    @Test
    public void testAllMonkeyRunnerCommandParametersWithCustomConfig() throws Exception {
        MonkeyRunnerMojo mojo = createMojo("monkey-runner-config-project2");

        // Mock MavenProject
        MavenProject project = Mockito.mock(MavenProject.class);
        setPrivateField(mojo, "project", project);
        Mockito.when(project.getBasedir()).thenReturn(new File("project/"));

        // Mock CommandExecutor
        CommandExecutor mockExecutor = Mockito.mock(CommandExecutor.class);
        Mockito.doNothing().when(mockExecutor).setLogger(Mockito.any());
        Mockito.doNothing().when(mockExecutor).setCustomShell(Mockito.any());
        Mockito.doNothing().when(mockExecutor).executeCommand(Mockito.anyString(), Mockito.anyList(), Mockito.eq(false));

        // Inject mock CommandExecutor
        setPrivateField(CommandExecutor.Factory.class, "executor", mockExecutor);

        // Execute mojo
        mojo.run(Mockito.mock(com.android.ddmlib.IDevice.class));

        // Capture the parameters passed to CommandExecutor
        @SuppressWarnings("unchecked") ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(mockExecutor).executeCommand(Mockito.anyString(), captor.capture(), Mockito.eq(false));

        // Verify parameters
        List<String> parameters = captor.getValue();
        List<String> expectedParameters = Arrays.asList("-plugin foo", "foo");
        assertEquals(expectedParameters, parameters);
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
