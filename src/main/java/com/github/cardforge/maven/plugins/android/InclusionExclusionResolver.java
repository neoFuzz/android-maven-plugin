package com.github.cardforge.maven.plugins.android;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import org.apache.maven.artifact.Artifact;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.FluentIterable.from;

/**
 * Utility class to resolve inclusion/exclusion patterns on artifact identifiers.
 * <p>
 * This class is used to resolve inclusion/exclusion patterns on artifact identifiers. It is used to determine whether an
 * artifact should be included or excluded from a list of artifacts. The patterns are specified as strings and are
 * converted to {@link Predicate}s using the {@code createPatternPredicate} method.
 * <p>
 * The patterns are specified as strings in the following format:
 * <pre>
 * +:&lt;
 * </pre>
 */
public class InclusionExclusionResolver {

    /**
     * Splitter that splits on ':' and trims the resulting strings.
     */
    private static final Splitter COLON_SPLITTER = Splitter.on(':');
    /**
     * Function that trims a string using {@link String#trim()}.
     * <p>
     * This is a {@link Function} implementation that is equivalent to the following:
     * <pre>
     * new Function&lt;
     */
    private static final Function<String, String> TRIMMER = String::trim;
    /**
     * Predicate that returns {@code true} if the input argument is not blank.
     * <p>
     * This is a {@link Predicate} implementation that is equivalent to the following:
     * <pre>
     * new Predicate&lt;
     */
    private static final Predicate<String> MUST_NOT_BE_BLANK = new Predicate<>() {
        /**
         * @param value the value to test
         * @return {@code true} if the value is not blank, {@code false} otherwise
         */
        @Override
        public boolean apply(@NonNull String value) {
            return !value.trim().isEmpty();
        }

        /**
         * @param input the input argument
         * @return {@code true} if the input argument matches the predicate, {@code false} otherwise
         */
        @Override
        public boolean test(@Nullable String input) {
            return this.apply(input);
        }
    };

    /**
     * Private constructor to prevent instantiation.
     */
    private InclusionExclusionResolver() {
    }

    /**
     * @param artifacts                 The artifacts to filter.
     * @param skipDependencies          Skip all dependencies, but respect {@code includeArtifactTypes}
     * @param includeArtifactTypes      Artifact types to be always included even if {@code skipDependencies} is
     *                                  {@code true}
     * @param excludeArtifactTypes      Artifact types to be always excluded even if {@code skipDependencies} is
     *                                  {@code false}
     * @param includeArtifactQualifiers Artifact qualifiers to be always included even if {@code skipDependencies} is
     *                                  {@code false}
     * @param excludeArtifactQualifiers Artifact qualifiers to be always excluded even if {@code skipDependencies} is
     *                                  {@code true}
     * @return The filtered artifacts.
     */
    @NonNull
    public static Collection<Artifact> filterArtifacts(@NonNull Iterable<Artifact> artifacts,
                                                       final boolean skipDependencies,
                                                       @Nullable final Collection<String> includeArtifactTypes,
                                                       @Nullable final Collection<String> excludeArtifactTypes,
                                                       @Nullable final Collection<String> includeArtifactQualifiers,
                                                       @Nullable final Collection<String> excludeArtifactQualifiers) {
        final boolean hasIncludeTypes = includeArtifactTypes != null;
        final boolean hasExcludeTypes = excludeArtifactTypes != null;
        final boolean hasIncludeQualifier = includeArtifactQualifiers != null;
        final boolean hasExcludeQualifier = excludeArtifactQualifiers != null;
        return from(artifacts)
                .filter(new Predicate<>() {
                    @Override
                    public boolean apply(Artifact artifact) {
                        final boolean includedByType = hasIncludeTypes &&
                                includeArtifactTypes.contains(artifact.getType());
                        final boolean includedByQualifier = hasIncludeQualifier
                                && match(artifact, includeArtifactQualifiers);
                        final boolean excludedByType = hasExcludeTypes
                                && excludeArtifactTypes.contains(artifact.getType());
                        final boolean excludedByQualifier = hasExcludeQualifier
                                && match(artifact, excludeArtifactQualifiers);

                        if (!skipDependencies) {
                            return !excludedByType && !excludedByQualifier
                                    || includedByQualifier
                                    || includedByType && !excludedByQualifier;
                        } else {
                            return includedByQualifier || includedByType;
                        }
                    }

                    @Override
                    public boolean test(@Nullable Artifact input) {
                        return this.apply(input);
                    }
                })
                .toSet();
    }

    /**
     * @param artifact           The artifact to match against the artifact qualifiers.
     * @param artifactQualifiers Artifact qualifiers in the format {@code groupId:artifactId:version}.
     * @return <code>true</code> if the artifact matches any of the artifact qualifiers, <code>false</code> otherwise
     */
    private static boolean match(final Artifact artifact, Iterable<String> artifactQualifiers) {
        return from(artifactQualifiers)
                .filter(MUST_NOT_BE_BLANK)
                .anyMatch(new Predicate<>() {
                    @Override
                    public boolean apply(String artifactQualifier) {
                        return match(artifact, artifactQualifier);
                    }

                    @Override
                    public boolean test(@Nullable String input) {
                        return this.apply(input);
                    }
                });
    }

    /**
     * @param artifact          The artifact to match against the artifact qualifier.
     * @param artifactQualifier The artifact qualifier in the format {@code groupId:artifactId:version}.
     * @return <code>true</code> if the artifact matches the artifact qualifier, <code>false</code> otherwise
     */
    private static boolean match(Artifact artifact, String artifactQualifier) {
        final List<String> split = from(COLON_SPLITTER.split(artifactQualifier)).transform(TRIMMER).toList();
        final int count = split.size();
        if (split.isEmpty() || count > 3) {
            throw new IllegalArgumentException("Invalid artifact qualifier: " + artifactQualifier);
        }
        // check groupId
        final String groupId = split.get(0);
        if (!groupId.equals(artifact.getGroupId())) {
            return false;
        }
        if (count == 1) {
            return true;
        }
        // check artifactId
        final String artifactId = split.get(1);
        if (!artifactId.equals(artifact.getArtifactId())) {
            return false;
        }
        if (count == 2) {
            return true;
        }
        // check version
        final String version = split.get(2);
        return version.equals(artifact.getVersion());
    }

}
