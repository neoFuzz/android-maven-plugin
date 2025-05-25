package com.github.cardforge.standalonemojos;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enhanced MavenProject implementation for testing with Maven 4.0.0+.
 * This replaces the old MavenProjectStub which is no longer available.
 */
public class MojoProjectStub extends MavenProject {
    private final File basedir;
    private final Properties props = new Properties();

    /**
     * Create a new MojoProjectStub with the given project directory.
     *
     * @param projectDir The directory containing the test project
     */
    public MojoProjectStub(File projectDir) {
        super(new Model());
        this.basedir = projectDir;
        props.setProperty("basedir", this.basedir.getAbsolutePath());

        // Look for either pom.xml or plugin-config.xml
        File pom = new File(getBasedir(), "pom.xml");
        if (!pom.exists()) {
            pom = new File(getBasedir(), "plugin-config.xml");
        }

        if (!pom.exists()) {
            throw new IllegalStateException("Could not find pom.xml or plugin-config.xml in " + getBasedir());
        }

        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model = null;
        FileReader fileReader = null;

        try {
            fileReader = new FileReader(pom);
            model = pomReader.read(fileReader);
            setModel(model);
        } catch (Exception e) {
            throw new RuntimeException("Error reading project model: " + e.getMessage(), e);
        } finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }

        // Set basic project information from the model
        setGroupId(model.getGroupId());
        setArtifactId(model.getArtifactId());
        setVersion(model.getVersion());
        setName(model.getName());
        setUrl(model.getUrl());
        setPackaging(model.getPackaging());
        setFile(pom);

        // Set up the build structure
        Build build = model.getBuild();
        if (build == null) {
            build = new Build();
            model.setBuild(build);
        }

        // Set standard directories
        setupStandardDirectories(build);

        // Set resources
        setupResources(build);
    }

    /**
     * Set up standard project directories
     */
    private void setupStandardDirectories(@Nonnull Build build) {
        // Source directory
        File srcDir = getStandardDir(build.getSourceDirectory(), "src/main/java");
        build.setSourceDirectory(srcDir.getAbsolutePath());

        // Build directory
        File targetDir = getStandardDir(build.getDirectory(), "target");
        build.setDirectory(targetDir.getAbsolutePath());

        // Output directory
        File outputDir = getStandardDir(build.getOutputDirectory(), "target/classes");
        build.setOutputDirectory(outputDir.getAbsolutePath());

        // Test directories
        File testSrcDir = getStandardDir(build.getTestSourceDirectory(), "src/test/java");
        build.setTestSourceDirectory(testSrcDir.getAbsolutePath());

        File testOutputDir = getStandardDir(build.getTestOutputDirectory(), "target/test-classes");
        build.setTestOutputDirectory(testOutputDir.getAbsolutePath());
    }

    /**
     * Set up project resources
     */
    private void setupResources(@Nonnull Build build) {
        List<Resource> resources = new ArrayList<>();

        // Add existing resources
        if (build.getResources() != null && !build.getResources().isEmpty()) {
            resources.addAll(build.getResources());

            // Normalize resource paths
            for (Resource resource : resources) {
                File dir = normalize(resource.getDirectory());
                resource.setDirectory(dir.getAbsolutePath());
                makeDirs(dir);
            }
        } else {
            // Add default resources directory if none defined
            Resource resource = new Resource();
            File resourceDir = normalize("src/main/resources");
            resource.setDirectory(resourceDir.getAbsolutePath());
            makeDirs(resourceDir);
            resources.add(resource);
        }

        build.setResources(resources);

        // Set up test resources
        List<Resource> testResources = new ArrayList<>();

        if (build.getTestResources() != null && !build.getTestResources().isEmpty()) {
            testResources.addAll(build.getTestResources());

            for (Resource resource : testResources) {
                File dir = normalize(resource.getDirectory());
                resource.setDirectory(dir.getAbsolutePath());
                makeDirs(dir);
            }
        } else {
            Resource testResource = new Resource();
            File testResourceDir = normalize("src/test/resources");
            testResource.setDirectory(testResourceDir.getAbsolutePath());
            makeDirs(testResourceDir);
            testResources.add(testResource);
        }

        build.setTestResources(testResources);
    }

    @Override
    public Properties getProperties() {
        return props;
    }

    @Override
    public File getBasedir() {
        return this.basedir;
    }

    /**
     * Get a standard directory, creating it if it doesn't exist
     */
    @Nonnull
    private File getStandardDir(String dirPath, String defaultPath) {
        File dir;

        if (StringUtils.isBlank(dirPath)) {
            dir = normalize(defaultPath);
        } else {
            dir = normalize(dirPath);
        }

        makeDirs(dir);
        return dir;
    }

    /**
     * Normalize a path.
     * Ensure path is absolute, and has proper system file separators.
     *
     * @param path the raw path
     * @return normalized File
     */
    @Nonnull
    private File normalize(final String path) {
        String ospath = FilenameUtils.separatorsToSystem(path);
        File file = new File(ospath);
        if (file.isAbsolute()) {
            return file;
        } else {
            return new File(getBasedir(), ospath);
        }
    }

    /**
     * Create directories if they don't exist
     */
    private void makeDirs(@Nonnull File dir) {
        if (dir.exists()) {
            return;
        }

        boolean created = dir.mkdirs();
        assertTrue(created, "Unable to create directories: " + dir);
    }
}