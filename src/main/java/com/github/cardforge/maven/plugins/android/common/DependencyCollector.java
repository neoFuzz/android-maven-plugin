package com.github.cardforge.maven.plugins.android.common;

import com.android.annotations.NonNull;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.logging.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Filters a graph of Artifacts and transforms it into a Set.
 */
final class DependencyCollector {
    private final Logger log;
    private final Set<Artifact> dependencies = new HashSet<>();
    private final Artifact target;

    /**
     * @param logger Logger on which to output and messages.
     * @param target Artifact from which we will start collecting.
     */
    DependencyCollector(Logger logger, Artifact target) {
        this.log = logger;
        this.target = target;
    }

    /**
     * Visits all nodes from the given node and collects dependencies.
     *
     * @param node       DependencyNode from which to search.
     * @param collecting Whether we are currently collecting artifacts.
     */
    public void visit(DependencyNode node, boolean collecting) {
        if (collecting) {
            dependencies.add(node.getArtifact());
        }

        if (matchesTarget(node.getArtifact())) {
            collecting = true;
            log.debug("Found target. Collecting dependencies after " + node.getArtifact());
        }

        for (final DependencyNode child : node.getChildren()) {
            visit(child, collecting);
        }
    }

    /**
     * Returns the set of dependencies found.
     * @return Set of dependencies found.
     */
    @NonNull
    public Set<Artifact> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    /**
     * Checks whether the given artifact matches the target.
     *
     * @param found Artifact to check for a match.
     * @return Whether the artifact matches the target.
     */
    private boolean matchesTarget(@NonNull Artifact found) {
        return found.getGroupId().equals(target.getGroupId())
                && found.getArtifactId().equals(target.getArtifactId())
                && found.getVersion().equals(target.getVersion())
                && found.getType().equals(target.getType())
                && classifierMatch(found.getClassifier(), target.getClassifier())
                ;
    }

    /**
     * Checks whether the two classifiers match.
     *
     * @param classifierA The classifier of the artifact we are looking for.
     * @param classifierB The classifier of the artifact we are looking for.
     * @return Whether the two classifiers match. If either classifier is null, it is considered a match.
     */
    private boolean classifierMatch(String classifierA, String classifierB) {
        final boolean hasClassifierA = !isNullOrEmpty(classifierA);
        final boolean hasClassifierB = !isNullOrEmpty(classifierB);
        if (!hasClassifierA && !hasClassifierB) {
            return true;
        } else if (hasClassifierA && hasClassifierB) {
            return classifierA.equals(classifierB);
        }
        return false;
    }

    /**
     * Checks whether the given string is null or empty.
     *
     * @param string String to check for null or empty
     * @return Whether the string is null or empty
     */
    private boolean isNullOrEmpty(String string) {
        return (string == null) || string.isEmpty();
    }
}
