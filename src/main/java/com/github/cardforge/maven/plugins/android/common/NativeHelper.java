package com.github.cardforge.maven.plugins.android.common;

import com.android.annotations.NonNull;
import com.github.cardforge.maven.plugins.android.AbstractAndroidMojo;
import com.github.cardforge.maven.plugins.android.AndroidNdk;
import com.google.common.io.PatternFilenameFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.*;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.CollectingDependencyNodeVisitor;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import static com.github.cardforge.maven.plugins.android.common.AndroidExtension.AAR;

/**
 * @author Johan Lindquist
 */
public class NativeHelper {

    /**
     * Message to be displayed when including attached artifacts.
     */
    public static final String FOR_NATIVE_ARTIFACTS = " for native artifacts";
    /**
     * Message to be displayed when including attached artifacts.
     */
    public static final String INC_ATT_ART = "Including attached artifact: ";
    public static final String ARMEABI = "armeabi";

    /**
     * The Maven project instance.
     */
    private final MavenProject project;
    /**
     * The dependency graph builder instance.
     */
    private final DependencyGraphBuilder dependencyGraphBuilder;
    /**
     * The logger instance.
     */
    private final Log log;

    private MavenSession session;

    /**
     * @param project                The Maven project instance.
     * @param dependencyGraphBuilder The dependency graph builder instance.
     * @param log                    The logger instance.
     */
    public NativeHelper(MavenProject project, DependencyGraphBuilder dependencyGraphBuilder,
                        Log log) {
        this.project = project;
        this.dependencyGraphBuilder = dependencyGraphBuilder;
        this.log = log;
    }

    /**
     * @param project                The Maven project instance.
     * @param dependencyGraphBuilder The dependency graph builder instance.
     * @param log                    The logger instance.
     * @param mavenSession           The Maven session to use.
     */
    public NativeHelper(MavenProject project, DependencyGraphBuilder dependencyGraphBuilder,
                        Log log, MavenSession mavenSession) {
        this.project = project;
        this.dependencyGraphBuilder = dependencyGraphBuilder;
        this.log = log;
        this.session = mavenSession;
    }

    /**
     * @param applicationMakefile The application makefile to retrieve the ABI from.
     * @return The ABI, or default to <code>armeabi</code> if not found
     */
    @Nonnull
    public static String[] getAppAbi(@NonNull File applicationMakefile) {
        try (Scanner scanner = new Scanner(applicationMakefile)) {
            if (applicationMakefile.exists()) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (line.startsWith("APP_ABI")) {
                        return line.substring(line.indexOf(":=") + 2).trim().split(" ");
                    }
                }
            }
        } catch (FileNotFoundException e) {
            // do nothing
        }

        // return a default ndk architecture
        return new String[]{ARMEABI};
    }

    /**
     * Extracts, if embedded correctly, the artifacts architecture from its classifier.  The format of the
     * classifier, if including the architecture is &lt;architecture&gt;-&lt;classifier&gt;.  If no
     * architecture is embedded in the classifier, 'armeabi' will be returned.
     *
     * @param artifact            The artifact to retrieve the classifier from.
     * @param defaultArchitecture The architecture to return if it can't be resolved from the classifier
     * @return The retrieved architecture, or <code>defaultArchitecture</code> if not resolvable
     */
    public static String extractArchitectureFromArtifact(@NonNull Artifact artifact, final String defaultArchitecture) {
        String classifier = artifact.getClassifier();
        if (classifier != null) {
            //
            // We loop backwards to catch the case where the classifier is
            // potentially armeabi-v7a - this collides with armeabi if looping
            // through this loop in the other direction
            //

            for (int i = AndroidNdk.NDK_ARCHITECTURES.length - 1; i >= 0; i--) {
                String ndkArchitecture = AndroidNdk.NDK_ARCHITECTURES[i];
                if (classifier.startsWith(ndkArchitecture)) {
                    return ndkArchitecture;
                }
            }

        }
        // Default case is to return the default architecture
        return defaultArchitecture;
    }

    /**
     * Attempts to extract, from various sources, the applicable list of NDK architectures to support
     * as part of the build.
     * <br>
     * <br>
     * It retrieves the list from the following places:
     * <ul>
     *     <li>ndkArchitecture parameter</li>
     *     <li>projects {@code Application.mk} - currently only a single architecture is supported by this method</li>
     * </ul>
     *
     * @param ndkArchitectures    Space separated list of architectures.  This may be from configuration or otherwise
     * @param applicationMakefile The makefile (Application.mk) to retrieve the list from.
     * @param basedir             Directory the build is running from (to resolve files)
     * @return The list of architectures to be supported by build.
     */
    @NonNull
    public static String[] getNdkArchitectures(final String ndkArchitectures, final String applicationMakefile,
                                               final File basedir) {
        // if there is a specified ndk architecture, return it
        if (ndkArchitectures != null) {
            return ndkArchitectures.split(" ");
        }

        // if there is no application makefile specified, let's use the default one
        String applicationMakefileToUse = applicationMakefile;
        if (applicationMakefileToUse == null) {
            applicationMakefileToUse = "jni/Application.mk";
        }

        // now let's see if the application file exists
        File appMK = new File(basedir, applicationMakefileToUse);
        if (appMK.exists()) {
            String[] foundNdkArchitectures = getAppAbi(appMK);
            if (!foundNdkArchitectures[0].equals(ARMEABI)) {
                return foundNdkArchitectures;
            }
        }

        // return a default ndk architecture
        return new String[]{ARMEABI};
    }

    /**
     * Helper method for determining whether the specified architecture is a match for the
     * artifact using its classifier.  When used for architecture matching, the classifier must be
     * formed by &lt;architecture&gt;-&lt;classifier&gt;.
     * If the artifact is legacy and defines no valid architecture, the artifact architecture will
     * default to <strong>armeabi</strong>.
     *
     * @param ndkArchitecture     Architecture to check for match
     * @param defaultArchitecture Default architecture to use if the artifact does not define one
     * @param artifact            Artifact to check the classifier match for
     * @return True if the architecture matches, otherwise false
     */
    public static boolean artifactHasHardwareArchitecture(@NonNull Artifact artifact, String ndkArchitecture,
                                                          String defaultArchitecture) {
        return Const.ArtifactType.NATIVE_SYMBOL_OBJECT.equals(artifact.getType())
                && ndkArchitecture.equals(extractArchitectureFromArtifact(artifact, defaultArchitecture));
    }

    /**
     * @param mojo            The mojo to use for logging
     * @param unpackDirectory The directory to unpack the artifacts to
     * @param sharedLibraries Whether to include shared libraries or not
     * @return Set of artifacts to include in the APK
     * @throws MojoExecutionException If there is a problem resolving the artifacts
     */
    public Set<Artifact> getNativeDependenciesArtifacts(
            AbstractAndroidMojo mojo, File unpackDirectory, boolean sharedLibraries)
            throws MojoExecutionException {
        log.debug("Finding native dependencies. UnpackFolder=" + unpackDirectory + " shared=" + sharedLibraries);
        final Set<Artifact> filteredArtifacts = new LinkedHashSet<>();
        final Set<Artifact> allArtifacts = new LinkedHashSet<>();

        // Add all dependent artifacts declared in the pom file
        // Note: The result of project.getDependencyArtifacts() can be an UnmodifiableSet so we
        //       have created our own above and add to that.
        allArtifacts.addAll(project.getDependencyArtifacts());

        // Add all transitive artifacts as well
        // this allows armeabi classifier -> apklib -> apklib -> apk chaining to only include armeabi in APK
        allArtifacts.addAll(project.getArtifacts());

        for (Artifact artifact : allArtifacts) {
            final boolean isNativeLibrary = isNativeLibrary(sharedLibraries, artifact.getType());

            log.debug("Checking artifact : " + artifact);
            // A null value in the scope indicates that the artifact has been attached
            // as part of a previous build step (NDK mojo)
            if (isNativeLibrary && artifact.getScope() == null) {
                // Including attached artifact
                log.debug(INC_ATT_ART + artifact + ". Artifact scope is not set.");
                filteredArtifacts.add(artifact);
            } else {
                if (isNativeLibrary && (
                        Artifact.SCOPE_COMPILE.equals(artifact.getScope()) || Artifact.SCOPE_RUNTIME
                                .equals(artifact.getScope()))) {
                    log.debug(INC_ATT_ART + artifact + ". Artifact scope is Compile or Runtime.");
                    filteredArtifacts.add(artifact);
                } else {
                    final String type = artifact.getType();

                    if (AAR.equals(type)) {
                        // Check if the artifact contains a libs folder - if so, include it in the list
                        final File libsFolder;
                        if (mojo != null) {
                            libsFolder = mojo.getUnpackedLibNativesFolder(artifact);
                        } else {
                            libsFolder = new File(
                                    new File(unpackDirectory.getAbsolutePath(), artifact.getArtifactId()),
                                    AAR.equals(type) ? "jni" : "libs");
                        }

                        if (!libsFolder.exists() || !libsFolder.isDirectory()) {
                            log.debug("Skipping " + libsFolder.getAbsolutePath() + FOR_NATIVE_ARTIFACTS);
                            continue;
                        }

                        log.debug("Checking " + libsFolder.getAbsolutePath() + FOR_NATIVE_ARTIFACTS);
                        // make sure we ignore libs folders that only contain JARs
                        // The regular expression filters out all file paths ending with '.jar' or '.JAR',
                        // so all native libs remain
                        if (libsFolder.list(new PatternFilenameFilter("^.*(?<!(?i)\\.jar)$")).length > 0) {
                            log.debug(INC_ATT_ART + artifact + ". Artifact is "
                                    + artifact.getType());
                            filteredArtifacts.add(artifact);
                        }
                    } else if (!"jar".equals(type)) {
                        log.debug("Not checking " + type + FOR_NATIVE_ARTIFACTS);
                    }
                }
            }
        }

        Set<Artifact> transitiveArtifacts = processTransitiveDependencies(project.getDependencies(), sharedLibraries);

        filteredArtifacts.addAll(transitiveArtifacts);

        return filteredArtifacts;
    }

    /**
     * @param sharedLibraries Whether to include shared libraries or not
     * @param artifactType    The artifact type to check
     * @return True if the artifact type is a native library, otherwise false
     */
    private boolean isNativeLibrary(boolean sharedLibraries, String artifactType) {
        return (sharedLibraries
                ? Const.ArtifactType.NATIVE_SYMBOL_OBJECT.equals(artifactType)
                : Const.ArtifactType.NATIVE_IMPLEMENTATION_ARCHIVE.equals(artifactType)
        );
    }

    /**
     * @param dependencies    The dependencies to process
     * @param sharedLibraries Whether to include shared libraries or not
     * @return Set of artifacts to include in the APK
     * @throws MojoExecutionException If there is a problem resolving the artifacts
     */
    @NonNull
    private Set<Artifact> processTransitiveDependencies(@NonNull List<Dependency> dependencies, boolean sharedLibraries)
            throws MojoExecutionException {
        final Set<Artifact> transitiveArtifacts = new LinkedHashSet<>();
        for (Dependency dependency : dependencies) {
            if (!Artifact.SCOPE_PROVIDED.equals(dependency.getScope()) && !dependency.isOptional()) {
                final Set<Artifact> transArtifactsFor = processTransitiveDependencies(dependency, sharedLibraries);
                log.debug("Found transitive dependencies for : " + dependency + " transDeps : " + transArtifactsFor);
                transitiveArtifacts.addAll(transArtifactsFor);
            }
        }

        return transitiveArtifacts;

    }

    /**
     * @param dependency      The dependency to process
     * @param sharedLibraries Whether to include shared libraries or not
     * @return Set of artifacts to include in the APK
     * @throws MojoExecutionException If there is a problem resolving the artifacts
     */
    @NonNull
    private Set<Artifact> processTransitiveDependencies(Dependency dependency, boolean sharedLibraries)
            throws MojoExecutionException {
        try {
            final Set<Artifact> artifacts = new LinkedHashSet<>();

            // Setup exclusion patterns
            final List<String> exclusionPatterns = new ArrayList<>();
            if (dependency.getExclusions() != null && !dependency.getExclusions().isEmpty()) {
                for (final Exclusion exclusion : dependency.getExclusions()) {
                    exclusionPatterns.add(exclusion.getGroupId() + ":" + exclusion.getArtifactId());
                }
            }

            // Setup filters
            final ArtifactFilter optionalFilter = artifact -> !artifact.isOptional();

            final AndArtifactFilter filter = new AndArtifactFilter();
            filter.add(new ExcludesArtifactFilter(exclusionPatterns));
            filter.add(new OrArtifactFilter(Arrays.asList(new ScopeArtifactFilter("compile"),
                    new ScopeArtifactFilter("runtime"),
                    new ScopeArtifactFilter("test"))));
            filter.add(optionalFilter);

            // Get ProjectBuildingRequest from MavenSession
            ProjectBuildingRequest request = session.getProjectBuildingRequest();// new DefaultProjectBuildingRequest()

            // Create a new request to avoid modifying the original
            DefaultProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(request);
            buildingRequest.setProject(project);
            buildingRequest.setRemoteRepositories(project.getRemoteArtifactRepositories());

            final DependencyNode node = dependencyGraphBuilder.buildDependencyGraph(buildingRequest, filter);
            final CollectingDependencyNodeVisitor collectingVisitor = new CollectingDependencyNodeVisitor();
            node.accept(collectingVisitor);

            final List<DependencyNode> dependencies = collectingVisitor.getNodes();
            for (final DependencyNode dep : dependencies) {
                final boolean isNativeLibrary = isNativeLibrary(sharedLibraries, dep.getArtifact().getType());
                if (isNativeLibrary) {
                    artifacts.add(dep.getArtifact());
                }
            }

            return artifacts;
        } catch (Exception e) {
            throw new MojoExecutionException("Error while processing transitive dependencies", e);
        }
    }

}
