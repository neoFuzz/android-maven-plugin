package com.github.cardforge;

import org.apache.maven.artifact.Artifact;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.LinkedHashSet;

import static com.github.neofuzz.InclusionExclusionResolver.filterArtifacts;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InclusionExclusionResolverTest {

    private static final Artifact A1 = artifact("jar", "G1", "A1", "1.0");
    private static final Artifact A2 = artifact("jar", "G2", "A1", "1.0");
    private static final Artifact A3 = artifact("aar", "G1", "A2", "1.0");
    private static final Artifact A4 = artifact("jar", "G1", "A3", "2.0-rc");
    private static final Artifact A5 = artifact("aar", "G2", "A2", "2.0-rc");

    private static final Collection<Artifact> ALL = collect(A1, A2, A3, A4, A5);
    private static final Collection<Artifact> NONE = emptySet();

    @Test
    public void testSkipDependenciesFalse() {
        assertEquals(
                "No artifacts must be skiped",
                ALL,
                filterArtifacts(ALL, false, null, null, null, null)
        );
    }

    @Test
    public void testSkipDependenciesTrue() {
        assertEquals(
                "All artifacts must be skipped",
                NONE,
                filterArtifacts(ALL, true, null, null, null, null)
        );
    }

    @Test
    public void testSkipDependenciesIncludeTypes() {
        assertEquals(
                "All artifacts must be skipped, but AAR artifacts have higher priority",
                collect(A3, A5),
                filterArtifacts(ALL, true, singleton("aar"), null, null, null)
        );
        assertEquals(
                "All artifacts must be skipped, but JAR artifacts have higher priority",
                collect(A1, A2, A4),
                filterArtifacts(ALL, true, singleton("jar"), null, null, null)
        );
        assertEquals(
                "No artifacts must be skipped",
                ALL,
                filterArtifacts(ALL, false, singleton("aar"), null, null, null)
        );
        assertEquals(
                "No artifacts must be skipped",
                ALL,
                filterArtifacts(ALL, false, singleton("jar"), null, null, null)
        );
    }

    @Test
    public void testSkipDependenciesExcludeTypes() {
        assertEquals(
                "All artifacts must be skipped, especially AAR artifacts",
                NONE,
                filterArtifacts(ALL, true, null, singleton("aar"), null, null)
        );
        assertEquals(
                "All artifacts must be skipped, especially JAR artifacts",
                NONE,
                filterArtifacts(ALL, true, null, singleton("jar"), null, null)
        );
        assertEquals(
                "AAR artifacts must be skipped",
                collect(A1, A2, A4),
                filterArtifacts(ALL, false, null, singleton("aar"), null, null)
        );
        assertEquals(
                "JAR artifacts must be skipped",
                collect(A3, A5),
                filterArtifacts(ALL, false, null, singleton("jar"), null, null)
        );
        assertEquals(
                "All artifacts must be skipped, especially both JAR and AAR artifacts",
                NONE,
                filterArtifacts(ALL, false, null, asList("aar", "jar"), null, null)
        );
    }

    @Test
    public void testMatchingArtifactTypesIncludeExcludePriority() {
        assertEquals(
                "Include must have higher priority",
                ALL,
                filterArtifacts(ALL, false, singleton("jar"), singleton("jar"), null, null)
        );
        assertEquals(
                "Include must have higher priority",
                collect(A1, A2, A4),
                filterArtifacts(ALL, false, singleton("jar"), asList("aar", "jar"), null, null)
        );
        assertEquals(
                "Include must have higher priority",
                collect(A1, A2, A4),
                filterArtifacts(ALL, true, singleton("jar"), singleton("jar"), null, null)
        );
        assertEquals(
                "Include must have higher priority",
                collect(A1, A2, A4),
                filterArtifacts(ALL, true, singleton("jar"), asList("aar", "jar"), null, null)
        );
    }

    @Test
    public void testIncludeExcludeByQualifiers() {
        assertEquals(
                "Empty exclude must do nothing",
                ALL,
                filterArtifacts(ALL, false, null, null, null, singleton(""))
        );
        assertEquals(
                "Empty include must do nothing",
                NONE,
                filterArtifacts(ALL, true, null, null, singleton(""), null)
        );
        assertEquals(
                "Skip all and must include all of group G2",
                collect(A2, A5),
                filterArtifacts(ALL, true, null, null, singleton("G2"), null)
        );
        assertEquals(
                "Skip all and must include all of group G2 and artifact A2",
                collect(A5),
                filterArtifacts(ALL, true, null, null, singleton("G2:A2"), null)
        );
        assertEquals(
                "Do not skip and must exclude group G2",
                collect(A1, A3, A4),
                filterArtifacts(ALL, false, null, null, null, singleton("G2"))
        );
        assertEquals(
                "Do not skip and must exclude group G2 and artifact A2",
                collect(A1, A2, A3, A4),
                filterArtifacts(ALL, false, null, null, null, singleton("G2:A2"))
        );
        assertEquals(
                "Do not skip and must exclude group G2 and artifact A2 with invalid version",
                ALL,
                filterArtifacts(ALL, false, null, null, null, singleton("G2:A2:-"))
        );
        assertEquals(
                "Do not skip and must exclude group G2 and artifact A2 with valid version",
                collect(A1, A2, A3, A4),
                filterArtifacts(ALL, false, null, null, null, singleton("G2:A2:2.0-rc"))
        );
    }

    @Test
    public void testIncludeExcludeTypeQualifierIntersections() {
        assertEquals(
                "Exclude all JARs but include by artifact qualifiers",
                collect(A2, A3, A4, A5),
                filterArtifacts(ALL, false, null, singleton("jar"), asList("G2:A1", "G1:A3"), null)
        );
        assertEquals(
                "Exclude all JARs but include by artifact qualifiers",
                collect(A2, A3, A4, A5),
                filterArtifacts(ALL, false, null, singleton("jar"), asList("G2:A1", "G1:A3"), asList("G2:A1", "G1:A3"))
        );
        assertEquals(
                "Skip all but must include all AAR files despite the concrete artifact exclusion",
                collect(A3, A5),
                filterArtifacts(ALL, true, singleton("aar"), null, null, singleton("G2:A2:2.0-rc"))
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalQualifier() {
        filterArtifacts(ALL, false, null, null, singleton("G1:A1:V:X"), null);
    }

    @Nonnull
    private static Collection<Artifact> collect(Artifact... artifacts) {
        return new LinkedHashSet<>(asList(artifacts));
    }

    @Nonnull
    private static Artifact artifact(String type, String groupId, String artifactId, String version) {
        final Artifact artifact = mock(Artifact.class);
        when(artifact.getType()).thenReturn(type);
        when(artifact.getGroupId()).thenReturn(groupId);
        when(artifact.getArtifactId()).thenReturn(artifactId);
        when(artifact.getVersion()).thenReturn(version);
        return artifact;
    }

}
