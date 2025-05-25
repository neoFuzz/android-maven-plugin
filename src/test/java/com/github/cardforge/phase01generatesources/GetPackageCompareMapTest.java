package com.github.cardforge.phase01generatesources;

import com.github.cardforge.maven.plugins.android.phase01generatesources.GenerateSourcesMojo;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers method {@link GenerateSourcesMojo#getPackageCompareMap(Set)} with tests
 *
 * @author Oleg Green - olegalex.green@gmail.com
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetPackageCompareMapTest {
    public static final String PROJECT_ARTIFACT_ID = "main_application";
    public static final String PROJECT_PACKAGE_NAME = "com.jayway.maven.application";
    public static final String COM_JAYWAY_MAVEN_LIBRARY_PACKAGE = "com.jayway.maven.library";
    public static final String COM_JAYWAY_MAVEN_LIBRARY2_PACKAGE = "com.jayway.maven.library2";
    public static final String COM_JAYWAY_MAVEN_LIBRARY3_PACKAGE = "com.jayway.maven.library3";
    public static final Artifact LIBRARY1_ARTIFACT = createArtifact("library1");
    public static final Artifact LIBRARY2_ARTIFACT = createArtifact("library2");
    public static final Artifact LIBRARY3_ARTIFACT = createArtifact("library3");
    public static final Map<Artifact, String> TEST_DATA_1 = new HashMap<>() {
        {
            put(LIBRARY1_ARTIFACT, COM_JAYWAY_MAVEN_LIBRARY_PACKAGE);
            put(LIBRARY2_ARTIFACT, PROJECT_PACKAGE_NAME);
            put(LIBRARY3_ARTIFACT, COM_JAYWAY_MAVEN_LIBRARY_PACKAGE);
        }
    };
    public static final Map<Artifact, String> TEST_DATA_2 = new HashMap<>() {
        {
            put(LIBRARY1_ARTIFACT, COM_JAYWAY_MAVEN_LIBRARY_PACKAGE);
            put(LIBRARY2_ARTIFACT, COM_JAYWAY_MAVEN_LIBRARY2_PACKAGE);
            put(LIBRARY3_ARTIFACT, COM_JAYWAY_MAVEN_LIBRARY3_PACKAGE);
        }
    };

    private MavenProject project;
    private Artifact projectArtifact;
    private GenerateSourcesMojo mojo;

    @Nonnull
    private static Artifact createArtifact(@Nonnull String artifactId) {
        Artifact artifactMock = Mockito.mock(Artifact.class);
        Mockito.when(artifactMock.getArtifactId()).thenReturn(artifactId);
        return artifactMock;
    }

    @BeforeAll
    public void setUp() throws Exception {
        //openMocks(this);

        mojo = Mockito.spy(new TestableGenerateSourcesMojo());
        setUpMainProject();

        // Inject the project field using reflection
        setPrivateField(mojo, "project", project);

        // Mock the method to always return PROJECT_PACKAGE_NAME
        Mockito.doAnswer(invocation -> PROJECT_PACKAGE_NAME)
                .when(mojo)
                .extractPackageNameFromAndroidManifest(Mockito.any(File.class));
    }

    @Test
    void testBasicManifest() {
        File basicManifest = new File(String.valueOf(getClass().getResource(
                "/manifest-tests/basic-android-project-manifest/AndroidManifest.xml").getPath()));
        GenerateSourcesMojo gsm = Mockito.spy(new GenerateSourcesMojo());

        String packageName = gsm.extractPackageNameFromAndroidManifest(basicManifest);
        assertNotNull(gsm);
        // This string comes from the file at */manifest-tests/basic-android-project-manifest/AndroidManifest.xml
        assertEquals("com.jayway.maven.plugins.android.tests", packageName);
    }

    @Test
    void testNoDependencies() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mojo.getPackageCompareMap(null)
        );
        Assertions.assertNotNull(exception);
    }

    @Test
    void testEmptyDependencies() throws MojoExecutionException {
        Map<String, Set<Artifact>> map = mojo.getPackageCompareMap(new HashSet<>());

        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(PROJECT_PACKAGE_NAME));

        Set<Artifact> artifactSet = map.get(PROJECT_PACKAGE_NAME);
        assertEquals(1, artifactSet.size());
        assertTrue(artifactSet.contains(projectArtifact));
    }

    @Test
    void testData1() throws Exception {
        mockExtractPackageNameFromArtifactMethod(TEST_DATA_1);

        Map<String, Set<Artifact>> map = mojo.getPackageCompareMap(TEST_DATA_1.keySet());

        assertNotNull(map);
        assertEquals(2, map.size());
        assertTrue(map.containsKey(PROJECT_PACKAGE_NAME));
        assertTrue(map.containsKey(COM_JAYWAY_MAVEN_LIBRARY_PACKAGE));

        Set<Artifact> artifactSet1 = map.get(PROJECT_PACKAGE_NAME);
        assertEquals(2, artifactSet1.size());
        assertTrue(artifactSet1.contains(LIBRARY2_ARTIFACT));
        assertTrue(artifactSet1.contains(projectArtifact));

        Set<Artifact> artifactSet2 = map.get(COM_JAYWAY_MAVEN_LIBRARY_PACKAGE);
        assertEquals(2, artifactSet2.size());
        assertTrue(artifactSet2.contains(LIBRARY1_ARTIFACT));
        assertTrue(artifactSet2.contains(LIBRARY3_ARTIFACT));
    }

    @Test
    void testData2() throws Exception {
        mockExtractPackageNameFromArtifactMethod(TEST_DATA_2);

        Map<String, Set<Artifact>> map = mojo.getPackageCompareMap(TEST_DATA_2.keySet());

        assertNotNull(map);
        assertEquals(4, map.size());
        assertTrue(map.containsKey(PROJECT_PACKAGE_NAME));

        Set<Artifact> artifactSet1 = map.get(PROJECT_PACKAGE_NAME);
        assertEquals(1, artifactSet1.size());
        assertTrue(artifactSet1.contains(projectArtifact));

        Set<Artifact> artifactSet2 = map.get(COM_JAYWAY_MAVEN_LIBRARY_PACKAGE);
        assertEquals(1, artifactSet2.size());
        assertTrue(artifactSet2.contains(LIBRARY1_ARTIFACT));

        Set<Artifact> artifactSet3 = map.get(COM_JAYWAY_MAVEN_LIBRARY2_PACKAGE);
        assertEquals(1, artifactSet3.size());
        assertTrue(artifactSet3.contains(LIBRARY2_ARTIFACT));

        Set<Artifact> artifactSet4 = map.get(COM_JAYWAY_MAVEN_LIBRARY3_PACKAGE);
        assertEquals(1, artifactSet4.size());
        assertTrue(artifactSet4.contains(LIBRARY3_ARTIFACT));
    }

    private void setUpMainProject() {
        projectArtifact = Mockito.mock(Artifact.class);
        Mockito.when(projectArtifact.getArtifactId()).thenReturn(PROJECT_ARTIFACT_ID);

        project = Mockito.mock(MavenProject.class);
        Mockito.when(project.getArtifact()).thenReturn(projectArtifact);
        Mockito.when(project.getGroupId()).thenReturn("com.jayway.maven");
    }

    private void mockExtractPackageNameFromArtifactMethod(final Map<Artifact, String> testData) throws Exception {
        Mockito.doAnswer(invocation -> {
            Artifact inputArtifact = invocation.getArgument(0);
            return testData.get(inputArtifact);
        }).when(mojo).extractPackageNameFromAndroidArtifact(Mockito.any(Artifact.class));

    }

    private void setPrivateField(@Nonnull Object target, String fieldName, Object value) throws Exception {
        Field field;
        Class<?> clazz = target.getClass();

        while (clazz != null) {  // Loop through superclasses
            try {
                field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;  // Stop once the field is found
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();  // Move to parent class
            }
        }

        throw new NoSuchFieldException("Field '" + fieldName + "' not found in " + target.getClass());
    }

    /**
     * Testing class to override the extractPackageNameFromAndroidManifest method.
     */
    class TestableGenerateSourcesMojo extends GenerateSourcesMojo {
        @Override
        public String extractPackageNameFromAndroidManifest(File manifestFile) {
            return PROJECT_PACKAGE_NAME;
        }
    }
}
