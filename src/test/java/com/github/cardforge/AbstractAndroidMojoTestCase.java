package com.github.cardforge;

import com.github.cardforge.maven.plugins.android.AbstractAndroidMojo;
import com.github.cardforge.maven.plugins.android.standalonemojos.ManifestMergerMojo;
import com.github.cardforge.standalonemojos.MojoProjectStub;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.DebugConfigurationListener;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.path.DefaultPathTranslator;
import org.apache.maven.project.path.PathTranslator;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.mockito.Mockito;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.reflect.Field;

import static org.mockito.Mockito.when;

public abstract class AbstractAndroidMojoTestCase<T extends AbstractAndroidMojo> extends AbstractMojoTestCase {

    protected MavenSession session;
    protected MojoExecution execution;

    /**
     * The Goal Name of the Plugin being tested.
     * <p>
     * Used to look for <code>&lt;configuration&gt;</code> section in the <code>plugin-config.xml</code>
     * that will be used to configure the mojo from.
     *
     * @return the string name of the goal. (eg "version-update", "dex", etc...)
     */
    public abstract String getPluginGoalName();

    /**
     * Copy the project specified into a temporary testing directory. Create the {@link MavenProject} and
     * {@link ManifestMergerMojo}, configure it from the <code>plugin-config.xml</code> and return the created Mojo.
     * <p>
     * Note: only configuration entries supplied in the plugin-config.xml are presently configured in the mojo returned.
     * That means and 'default-value' settings are not automatically injected by this testing framework (or plexus
     * underneath that is supplying this functionality)
     *
     * @param resourceProject the name of the goal to look for in the <code>plugin-config.xml</code> that the configuration will be
     *                        pulled from.
     * @return the created mojo (un-executed)
     * @throws Exception if there was a problem creating the mojo.
     */
    protected T createMojo(String resourceProject) throws Exception {
        // Establish test details project example
        String testResourcePath = "src/test/resources/" + resourceProject;
        testResourcePath = FilenameUtils.separatorsToSystem(testResourcePath);
        File exampleDir = new File(getBasedir(), testResourcePath);
        assertTrue("Path should exist: " + exampleDir, exampleDir.exists());

        // Establish the temporary testing directory
        String testingPath = "target/tests/" + this.getClass().getSimpleName() + "." + getName();
        testingPath = FilenameUtils.separatorsToSystem(testingPath);
        File testingDir = new File(getBasedir(), testingPath);

        if (testingDir.exists()) {
            FileUtils.cleanDirectory(testingDir);
        } else {
            assertTrue("Could not create directory: " + testingDir, testingDir.mkdirs());
        }

        // Copy project example into temporary testing directory
        FileUtils.copyDirectory(exampleDir, testingDir);

        // Prepare MavenProject
        final MavenProject project = new MojoProjectStub(testingDir);

        // Setup Mojo
        PlexusConfiguration config = extractPluginConfiguration("android-maven-plugin", project.getFile());
        @SuppressWarnings("unchecked") final T mojo = (T) lookupMojo(getPluginGoalName(), project.getFile()); // TODO: whatever it is, this line causes all the tests to fail

        // Inject project itself
        setVariableValueToObject(mojo, "project", project);

        // Configure mocks for the session
        MavenSession context = Mockito.mock(MavenSession.class);
        when(context.getExecutionProperties()).thenReturn(project.getProperties());
        when(context.getCurrentProject()).thenReturn(project);

        // Configure the rest of the pieces via the PluginParameterExpressionEvaluator
        MojoDescriptor mojoDesc = new MojoDescriptor();
        mojoDesc.setGoal(getPluginGoalName());
        MojoExecution mojoExec = new MojoExecution(mojoDesc);

        PathTranslator pathTranslator = new DefaultPathTranslator();
        Logger logger = new ConsoleLogger(Logger.LEVEL_DEBUG, mojo.getClass().getName());

        ExpressionEvaluator evaluator = new PluginParameterExpressionEvaluator(
                context, mojoExec, pathTranslator, logger, project, project.getProperties()
        );

        ComponentConfigurator configurator = (ComponentConfigurator) lookup(ComponentConfigurator.ROLE, "basic");
        ConfigurationListener listener = new DebugConfigurationListener(logger);
        configurator.configureComponent(mojo, config, evaluator, getContainer().getContainerRealm(), listener);

        // Inject session and execution using reflection
        injectField(mojo, "session", context);
        injectField(mojo, "execution", mojoExec);

        this.session = context;
        this.execution = mojoExec;

        return mojo;
    }

    /**
     * Utility method to inject a private field using reflection.
     *
     * @param target    The object to modify.
     * @param fieldName The name of the field to inject.
     * @param value     The value to set for the field.
     * @throws Exception if the field cannot be modified.
     */
    private void injectField(@Nonnull Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Get the project directory used for this mojo.
     *
     * @param mojo the mojo to query.
     * @return the project directory.
     * @throws IllegalAccessException if unable to get the project directory.
     */
    public File getProjectDir(AbstractAndroidMojo mojo) throws IllegalAccessException {
        MavenProject project = (MavenProject) getVariableValueFromObject(mojo, "project");
        return project.getFile().getParentFile();
    }
}
