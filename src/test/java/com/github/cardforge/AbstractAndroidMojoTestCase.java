package com.github.cardforge;

import com.github.cardforge.maven.plugins.android.AbstractAndroidMojo;
import com.github.cardforge.maven.plugins.android.standalonemojos.ManifestMergerMojo;
import com.github.cardforge.standalonemojos.MojoProjectStub;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base test class for Android Mojo tests compatible with Maven
 */
public abstract class AbstractAndroidMojoTestCase<T extends AbstractAndroidMojo> {

    protected MavenSession session;
    protected MojoExecution execution;
    protected PlexusContainer container;

    @TempDir
    protected File tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        // Create a new PlexusContainer for component lookup
        container = new DefaultPlexusContainer();
    }

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
        assertTrue(exampleDir.exists(), "Path should exist: " + exampleDir);

        // Establish the temporary testing directory
        String testingPath = tempDir.getAbsolutePath() + File.separator + this.getClass().getSimpleName();
        File testingDir = new File(testingPath);

        if (testingDir.exists()) {
            FileUtils.cleanDirectory(testingDir);
        } else {
            assertTrue(testingDir.mkdirs(), "Could not create directory: " + testingDir);
        }

        // Copy project example into temporary testing directory
        FileUtils.copyDirectory(exampleDir, testingDir);

        // Prepare MavenProject
        MavenProject project = createMavenProject(testingDir);

        // Create the mojo instance via reflection
        Class<T> mojoClass = getMojoClass();
        T mojo;
        try {
            mojo = mojoClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            System.out.println("Failed to create mojo instance: " + mojoClass.getName());
            mojo = (T) mojoClass.getDeclaredConstructors()[0].newInstance();
        }

        // Set up the plugin descriptor and mojo descriptor
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("com.github.cardforge");
        pluginDescriptor.setArtifactId("android-maven-plugin");
        pluginDescriptor.setVersion("4.7.1");

        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setGoal(getPluginGoalName());
        mojoDescriptor.setImplementation(getMojoClass().getName());
        mojoDescriptor.setPluginDescriptor(pluginDescriptor);

        // Set up repository session
        RepositorySystemSession repoSession = new DefaultRepositorySystemSession();

        // Set up Maven Execution Request
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setSystemProperties(System.getProperties());
        request.setUserProperties(new Properties());
        request.setBaseDirectory(project.getBasedir());

        // Set up Project Building Request
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
        buildingRequest.setRepositorySession(repoSession);
        buildingRequest.setSystemProperties(System.getProperties());

        // Create Maven Session
        MavenSession mavenSession = new MavenSession(container, repoSession, request, new DefaultMavenExecutionResult());

        mavenSession.setCurrentProject(project);
        mavenSession.setProjects(new ArrayList<>() {{
            add(project);
        }});

        // Create Mojo Execution
        MojoExecution mojoExecution = new MojoExecution(mojoDescriptor);
        // Set up logger
        Logger logger = LoggerFactory.getLogger(AbstractAndroidMojo.class);

        // Inject all required components
        injectField(mojo, "project", project);
        injectField(mojo, "session", mavenSession);
        injectField(mojo, "execution", mojoExecution);

        // Try to inject logger if it has one
        try {
            Field logField = AbstractAndroidMojo.class.getDeclaredField("log");
            logField.setAccessible(true);

            // Remove final modifier
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(logField, logField.getModifiers() & ~Modifier.FINAL);

            // Set the logger
            logField.set(mojo, logger);
        } catch (Exception e) {
            // Handle or log exception
        }

        // Store references for test assertions
        this.session = mavenSession;
        this.execution = mojoExecution;

        // Configure the mojo from plugin configuration
        configureFromPom(mojo, testingDir);

        return mojo;
    }

    /**
     * Create a Maven project for testing
     */
    protected MavenProject createMavenProject(File projectDir) {
        return new MojoProjectStub(projectDir);
    }

    /**
     * Configure the mojo from plugin configuration in pom.xml
     * This method should be implemented based on your specific configuration needs
     */
    protected void configureFromPom(T mojo, File projectDir) throws Exception {
        // Basic implementation - extend in subclasses for specific configs
        // This method would parse your plugin-config.xml and set values on the mojo

        // Example of setting fields manually:
        // setField(mojo, "androidManifestFile", new File(projectDir, "AndroidManifest.xml"));
        // setField(mojo, "resourceDirectory", new File(projectDir, "res"));
    }

    /**
     * Get the mojo class
     */
    protected abstract Class<T> getMojoClass();

    /**
     * Utility method to inject a private field using reflection.
     *
     * @param target    The object to modify.
     * @param fieldName The name of the field to inject.
     * @param value     The value to set for the field.
     * @throws Exception if the field cannot be modified.
     */
    private void injectField(@Nonnull Object target, String fieldName, Object value) throws Exception {
        Field field = getFieldRecursively(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Nonnull
    private Field getFieldRecursively(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field '" + fieldName + "' not found in class hierarchy.");
    }

    /**
     * Set a field value by name
     */
    protected void setField(Object object, String fieldName, Object value) throws Exception {
        injectField(object, fieldName, value);
    }

    /**
     * Get the project directory used for this mojo.
     *
     * @param mojo the mojo to query.
     * @return the project directory.
     * @throws IllegalAccessException if unable to get the project directory.
     */
    public File getProjectDir(@Nonnull AbstractAndroidMojo mojo) throws Exception {
        Field projectField = getFieldRecursively(mojo.getClass(), "project");
        projectField.setAccessible(true);
        MavenProject project = (MavenProject) projectField.get(mojo);
        return project.getFile().getParentFile();
    }

    /**
     * Get the base directory of the project
     */
    protected File getBasedir() {
        return new File(System.getProperty("basedir", System.getProperty("user.dir")));
    }

    /**
     * Get the value of a variable from the mojo
     *
     * @param o         the mojo to query.
     * @param fieldName the name of the variable to get.
     * @return the value of the variable.
     * @throws Exception if unable to get the value of the variable.
     */
    protected Object getVariableValueFromObject(@Nonnull Object o, String fieldName) throws Exception {
        Field field = getFieldRecursively(o.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(o);
    }
}
