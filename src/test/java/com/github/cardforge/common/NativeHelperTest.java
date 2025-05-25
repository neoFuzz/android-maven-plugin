package com.github.cardforge.common;

import com.github.cardforge.maven.plugins.android.AndroidNdk;
import com.github.cardforge.maven.plugins.android.common.Const;
import com.github.cardforge.maven.plugins.android.common.NativeHelper;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.DefaultProjectDependenciesResolver;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import static org.mockito.Mockito.mock;

/**
 * @author Johan Lindquist
 */
public class NativeHelperTest {
    private static NativeHelper nativeHelper;

    @Rule
    public TemporaryFolder apklibDir = new TemporaryFolder();

    @BeforeAll
    public static void setupNativeHelper() {
        MavenProject project = new MavenProject();
        project.setDependencyArtifacts(Collections.emptySet());

        Artifact apklib = new DefaultArtifact(
                "group",
                "some-apklib",
                "version",
                "scope",
                "aar",
                "classifier",
                null);
        project.addAttachedArtifact(apklib);

        Log mockLog = mock(Log.class);
        final ProjectDependenciesResolver resolver = new DefaultProjectDependenciesResolver();
        nativeHelper = new NativeHelper(project, (DependencyGraphBuilder) resolver, mockLog);

    }

    @Test
    public void shouldNotIncludeLibsFolderAsNativeDependenciesSourceWhenNoNativeLibsInside() throws Exception {
        apklibDir.create();
        new File(apklibDir.getRoot(), "some-apklib/libs").mkdirs();
        new File(apklibDir.getRoot(), "some-apklib/libs/some.jar").createNewFile();

        Set<Artifact> nativeDependencies = nativeHelper.getNativeDependenciesArtifacts(null, apklibDir.getRoot(), true);

        Assertions.assertTrue(nativeDependencies.isEmpty(), "Included JARs as native dependencies, but shouldn't");
    }

    @Test
    public void shouldIncludeLibsFolderAsNativeDependenciesSourceWhenNativeLibsInside() throws Exception {
        apklibDir.create();
        new File(apklibDir.getRoot(), "some-apklib/libs").mkdirs();
        new File(apklibDir.getRoot(), "some-apklib/libs/some.jar").createNewFile();
        new File(apklibDir.getRoot(), "some-apklib/libs/some.so").createNewFile();

        Set<Artifact> nativeDependencies = nativeHelper.getNativeDependenciesArtifacts(null, apklibDir.getRoot(), true);

        Assertions.assertTrue(nativeDependencies.isEmpty(), "Included attached native artifacts, but shouldn't");
    }

    @Test
    public void architectureResolutionForPlainArchitectureClassifier() {
        for (String ndkArchitecture : AndroidNdk.NDK_ARCHITECTURES) {
            Artifact artifact = new DefaultArtifact("acme", "acme", "1.0", "runtime", Const.ArtifactType.NATIVE_SYMBOL_OBJECT, ndkArchitecture, null);
            String architecture = NativeHelper.extractArchitectureFromArtifact(artifact, "armeabi");
            Assertions.assertNotNull(architecture, "unexpected null architecture");
            Assertions.assertEquals(ndkArchitecture, architecture, "unexpected architecture");
        }

    }

    @Test
    public void architectureResolutionForMixedArchitectureClassifier() {
        for (String ndkArchitecture : AndroidNdk.NDK_ARCHITECTURES) {
            Artifact artifact = new DefaultArtifact("acme", "acme", "1.0", "runtime", Const.ArtifactType.NATIVE_SYMBOL_OBJECT, ndkArchitecture + "-acme", null);
            String architecture = NativeHelper.extractArchitectureFromArtifact(artifact, "armeabi");
            Assertions.assertNotNull(architecture, "unexpected null architecture");
            Assertions.assertEquals(ndkArchitecture, architecture, "unexpected architecture");
        }
    }

    @Test
    public void architectureResolutionForDefaultLegacyArchitectureClassifier() {
        Artifact artifact = new DefaultArtifact("acme", "acme", "1.0", "runtime", Const.ArtifactType.NATIVE_SYMBOL_OBJECT, "acme", null);
        String architecture = NativeHelper.extractArchitectureFromArtifact(artifact, "armeabi");
        Assertions.assertNotNull(architecture, "unexpected null architecture");
        Assertions.assertEquals("armeabi", architecture, "unexpected architecture");
    }

    @Test
    public void artifactHasHardwareArchitecture() {
        for (String ndkArchitecture : AndroidNdk.NDK_ARCHITECTURES) {
            Artifact artifact = new DefaultArtifact("acme", "acme", "1.0", "runtime", Const.ArtifactType.NATIVE_SYMBOL_OBJECT, ndkArchitecture, null);
            boolean value = NativeHelper.artifactHasHardwareArchitecture(artifact, ndkArchitecture, "armeabi");
            Assertions.assertTrue(value, "unexpected value");
        }
    }

    @Test
    public void artifactHasHardwareArchitectureWithClassifier() {
        for (String ndkArchitecture : AndroidNdk.NDK_ARCHITECTURES) {
            Artifact artifact = new DefaultArtifact("acme", "acme", "1.0", "runtime", Const.ArtifactType.NATIVE_SYMBOL_OBJECT, ndkArchitecture + "-acme", null);
            boolean value = NativeHelper.artifactHasHardwareArchitecture(artifact, ndkArchitecture, "armeabi");
            Assertions.assertTrue(value, "unexpected value");
        }
    }

    @Test
    public void artifactHasHardwareArchitectureWithDefaultLegacyClassifier() {
        Artifact artifact = new DefaultArtifact("acme", "acme", "1.0", "runtime", Const.ArtifactType.NATIVE_SYMBOL_OBJECT, "acme", null);
        boolean value = NativeHelper.artifactHasHardwareArchitecture(artifact, "armeabi", "armeabi");
        Assertions.assertTrue(value, "unexpected value");
    }

    @Test
    public void artifactHasHardwareArchitectureNotNativeLibrary() {
        Artifact artifact = new DefaultArtifact("acme", "acme", "1.0", "runtime", "jar", "armeabi", null);
        boolean value = NativeHelper.artifactHasHardwareArchitecture(artifact, "armeabi", "armeabi");
        Assertions.assertFalse(value, "unexpected value");
    }

}
