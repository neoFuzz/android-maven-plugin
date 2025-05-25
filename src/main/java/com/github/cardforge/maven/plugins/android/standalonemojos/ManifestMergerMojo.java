package com.github.cardforge.maven.plugins.android.standalonemojos;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.dependency.level2.AndroidDependency;
import com.android.manifmerger.*;
import com.android.utils.ILogger;
import com.github.cardforge.maven.plugins.android.AbstractAndroidMojo;
import com.github.cardforge.maven.plugins.android.common.AndroidExtension;
import com.github.cardforge.maven.plugins.android.configuration.ManifestMerger;
import com.github.cardforge.maven.plugins.android.configuration.UsesSdk;
import com.github.cardforge.maven.plugins.android.configuration.VersionGenerator;
import com.github.cardforge.maven.plugins.android.phase01generatesources.MavenILogger;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Manifest Merger V2 <code>AndroidManifest.xml</code> file.
 * <a href="http://tools.android.com/tech-docs/new-build-system/user-guide/manifest-merger">User Guide</a>
 *
 * @author Benoit Billington - benoit.billington@gmail.com
 */
@Mojo(name = "manifest-merger", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class ManifestMergerMojo extends AbstractAndroidMojo {

    /**
     * Static string for when manifest is merged
     */
    public static final String MERGED_MANIFEST_SAVED = "Merged manifest saved to ";
    /**
     * Static string message for unhandled result type.
     */
    public static final String UNHANDLED_RESULT_TYPE = "Unhandled result type : ";

    /**
     * Update the <code>android:versionName</code> with the specified parameter. If left empty it
     * will use the version number of the project. Exposed via the project property
     * <code>android.manifestMerger.versionName</code>.
     */
    @Parameter(property = "android.manifestMerger.versionName", defaultValue = "${project.version}")
    protected String manifestVersionName;
    /**
     * Update the <code>android:versionCode</code> attribute with the specified parameter. Exposed via
     * the project property <code>android.manifestMerger.versionCode</code>.
     */
    @Parameter(property = "android.manifestMerger.versionCode", defaultValue = "1")
    protected Integer manifestVersionCode;
    /**
     * Update the <code>android:versionCode</code> attribute automatically from the project version
     * e.g 3.2.1 will become version code 3002001. As described in this
     * <a href="http://www.simpligility.com/2010/11/release-version-management-for-your-android-application/">blog post</a>
     * but done without using resource filtering. The value is exposed via the project property
     * <code>android.manifest.versionCodeUpdateFromVersion</code> and the resulting value
     * as <code>android.manifest.versionCode</code>.
     * For the purpose of generating the versionCode, if a version element is missing it is presumed to be 0.
     * The maximum values for the version increment and version minor values are 999,
     * the version major should be no larger than 2000.  Any other suffixes do not
     * participate in the version code generation.
     */
    @Parameter(property = "android.manifest.versionCodeUpdateFromVersion", defaultValue = "false")
    protected Boolean manifestVersionCodeUpdateFromVersion = false;
    /**
     * Optionally use a pattern to match version elements for automatic generation of version codes,
     * useful in case of complex version naming schemes. The new behavior is disabled by default;
     * set the pattern to a non-empty string to activate. Otherwise, continue using the old
     * behavior of separating version elements by dots and ignoring all non-digit characters.
     * The pattern is standard Java regex. Capturing groups in the pattern are sequentially passed
     * to the version code generator, while other parts are ignored. Be sure to properly escape
     * your pattern string, in case you use characters that have special meaning in XML.
     * Exposed via the project property
     * <code>android.manifestMerger.versionNamingPattern</code>.
     */
    @Parameter(property = "android.manifestMerger.versionNamingPattern")
    protected String manifestVersionNamingPattern;
    /**
     * The number of digits per version element. Must be specified as a comma/semicolon separated list of
     * digits, one for each version element, Exposed via the project property
     * <code>android.manifestMerger.versionDigits</code>.
     */
    @Parameter(property = "android.manifestMerger.versionDigits", defaultValue = "4,3,3")
    protected String manifestVersionDigits;
    /**
     * Merge Manifest with library projects. Exposed via the project property
     * <code>android.manifestMerger.mergeLibraries</code>.
     */
    @Parameter(property = "android.manifestMerger.mergeLibraries", defaultValue = "false")
    protected Boolean manifestMergeLibraries;
    /**
     * Merge Manifest with library projects. Exposed via the project property
     * <code>android.manifestMerger.mergeLibraries</code>.
     */
    @Parameter(property = "android.manifestMerger.mergeReportFile")
    protected File manifestMergeReportFile;
    /**
     * Update the uses-sdk tag. It can be configured to change: <code>android:minSdkVersion</code>,
     * <code>android:maxSdkVersion</code> and <code>android:targetSdkVersion</code>
     */
    protected UsesSdk manifestUsesSdk;
    /**
     * Configuration for the manifest-update goal.
     * <p>
     * You can configure this mojo to update the following basic manifestMerger attributes:
     * </p>
     * <p>
     * <code>android:versionName</code> on the <code>manifestMerger</code> element.
     * <code>android:versionCode</code> on the <code>manifestMerger</code> element.
     * </p>
     * <p>
     * You can configure attributes in the plugin configuration like so
     *
     * <pre>
     *   &lt;plugin&gt;
     *     &lt;groupId&gt;com.jayway.maven.plugins.android.generation2&lt;/groupId&gt;
     *     &lt;artifactId&gt;android-maven-plugin&lt;/artifactId&gt;
     *     &lt;executions&gt;
     *       &lt;execution&gt;
     *         &lt;id&gt;merge-manifest&lt;/id&gt;
     *         &lt;goals&gt;
     *           &lt;goal&gt;manifest-merger&lt;/goal&gt;
     *         &lt;/goals&gt;
     *         &lt;configuration&gt;
     *           &lt;manifestMerger&gt;
     *             &lt;versionName&gt;&lt;/versionName&gt;
     *             &lt;versionCode&gt;123&lt;/versionCode&gt;
     *             &lt;versionCodeUpdateFromVersion&gt;true|false&lt;/versionCodeUpdateFromVersion&gt;
     *             &lt;versionNamingPattern&gt;&lt;/versionNamingPattern&gt;
     *             &lt;mergeLibraries&gt;true|false&lt;/mergeLibraries&gt;
     *             &lt;mergeReportFile&gt;${project.build.directory}/ManifestMergeReport.txt&lt;/mergeReportFile&gt;
     *             &lt;usesSdk&gt;
     *               &lt;minSdkVersion&gt;14&lt;/minSdkVersion&gt;
     *               &lt;targetSdkVersion&gt;21&lt;/targetSdkVersion&gt;
     *             &lt;/usesSdk&gt;
     *           &lt;/manifestMerger&gt;
     *         &lt;/configuration&gt;
     *       &lt;/execution&gt;
     *     &lt;/executions&gt;
     *   &lt;/plugin&gt;
     * </pre>
     * <p>
     * or use properties set in the pom or settings file or supplied as command line parameter. Add
     * "android." in front of the property name for command line usage. All parameters follow a
     * manifestMerger.* naming convention.
     */
    @Parameter
    private ManifestMerger manifestMerger;
    private Boolean parsedVersionCodeUpdateFromVersion;
    private String parsedVersionNamingPattern;
    private String parsedVersionDigits;
    private Boolean parsedMergeLibraries;
    private String parsedVersionName;
    private Integer parsedVersionCode;
    private UsesSdk parsedUsesSdk;
    private File parsedMergeReportFile;

    /**
     * Sets the {@link com.android.manifmerger.ManifestSystemProperty} that can be injected
     * in the manifest file.
     */
    private static void setInjectableValues(
            ManifestMerger2.Invoker<?> invoker,
            String packageOverride,
            int versionCode,
            String versionName,
            @Nullable String minSdkVersion,
            @Nullable String targetSdkVersion,
            @Nullable Integer maxSdkVersion) {

        if (!Strings.isNullOrEmpty(packageOverride)) {
            invoker.setOverride(ManifestSystemProperty.PACKAGE, packageOverride);
        }
        if (versionCode > 0) {
            invoker.setOverride(ManifestSystemProperty.VERSION_CODE,
                    String.valueOf(versionCode));
        }
        if (!Strings.isNullOrEmpty(versionName)) {
            invoker.setOverride(ManifestSystemProperty.VERSION_NAME, versionName);
        }
        if (!Strings.isNullOrEmpty(minSdkVersion)) {
            invoker.setOverride(ManifestSystemProperty.MIN_SDK_VERSION, minSdkVersion);
        }
        if (!Strings.isNullOrEmpty(targetSdkVersion)) {
            invoker.setOverride(ManifestSystemProperty.TARGET_SDK_VERSION, targetSdkVersion);
        }
        if (maxSdkVersion != null) {
            invoker.setOverride(ManifestSystemProperty.MAX_SDK_VERSION, maxSdkVersion.toString());
        }
    }

    /**
     * Execute.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException the mojo execution exception
     * @throws org.apache.maven.plugin.MojoFailureException   the mojo failure exception
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!AndroidExtension.isAndroidPackaging(project.getPackaging())) {
            return; // skip, not an android project.
        }

        if (androidManifestFile == null) {
            getLog().debug("skip, no androidmanifest.xml defined (androidManifestFile rare case)");
            return; // skip, no androidmanifest.xml defined (rare case)
        }

        parseConfiguration();

        getLog().info("Attempting to update manifest " + androidManifestFile);
        getLog().debug("    usesSdk=" + parsedUsesSdk);
        getLog().debug("    versionName=" + parsedVersionName);
        getLog().debug("    versionCode=" + parsedVersionCode);
        getLog().debug("    versionCodeUpdateFromVersion=" + parsedVersionCodeUpdateFromVersion);
        getLog().debug("    versionNamingPattern=" + parsedVersionNamingPattern);
        getLog().debug("    versionDigits=" + parsedVersionDigits);
        getLog().debug("    mergeLibraries=" + parsedMergeLibraries);
        getLog().debug("    mergeReportFile=" + parsedMergeReportFile);

        if (!androidManifestFile.exists()) {
            return; // skip, no AndroidManifest.xml file found.
        }

        getLog().debug("Using manifest merger V2");
        manifestMergerV2();
    }

    private void parseConfiguration() {
        // manifestMerger element found in plugin config in pom
        if (manifestMerger != null) {
            if (StringUtils.isNotEmpty(manifestMerger.getVersionName())) {
                parsedVersionName = manifestMerger.getVersionName();
            } else {
                parsedVersionName = manifestVersionName;
            }
            if (manifestMerger.getVersionCode() != null) {
                parsedVersionCode = manifestMerger.getVersionCode();
            } else {
                parsedVersionCode = manifestVersionCode;
            }
            if (manifestMerger.getVersionCodeUpdateFromVersion() != null) {
                parsedVersionCodeUpdateFromVersion = manifestMerger.getVersionCodeUpdateFromVersion();
            } else {
                parsedVersionCodeUpdateFromVersion = manifestVersionCodeUpdateFromVersion;
            }
            if (manifestMerger.getVersionNamingPattern() != null) {
                parsedVersionNamingPattern = manifestMerger.getVersionNamingPattern();
            } else {
                parsedVersionNamingPattern = manifestVersionNamingPattern;
            }
            if (manifestMerger.getVersionDigits() != null) {
                parsedVersionDigits = manifestMerger.getVersionDigits();
            } else {
                parsedVersionDigits = manifestVersionDigits;
            }
            if (manifestMerger.getUsesSdk() != null) {
                parsedUsesSdk = manifestMerger.getUsesSdk();
            } else {
                parsedUsesSdk = manifestUsesSdk;
            }
            if (manifestMerger.getMergeLibraries() != null) {
                parsedMergeLibraries = manifestMerger.getMergeLibraries();
            } else {
                parsedMergeLibraries = manifestMergeLibraries;
            }
            if (manifestMerger.getMergeReportFile() != null) {
                parsedMergeReportFile = manifestMerger.getMergeReportFile();
            } else {
                parsedMergeReportFile = manifestMergeReportFile;
            }
        } else {
            parsedVersionName = manifestVersionName;
            parsedVersionCode = manifestVersionCode;
            parsedUsesSdk = manifestUsesSdk;
            parsedVersionCodeUpdateFromVersion = manifestVersionCodeUpdateFromVersion;
            parsedVersionNamingPattern = manifestVersionNamingPattern;
            parsedVersionDigits = manifestVersionDigits;
            parsedMergeLibraries = manifestMergeLibraries;
            parsedMergeReportFile = manifestMergeReportFile;
        }
    }

    /**
     * @throws MojoExecutionException the mojo execution exception
     */
    public void manifestMergerV2() throws MojoExecutionException {
        ILogger logger = new MavenILogger(getLog(), (parsedMergeReportFile != null));
//        AndroidBuilder builder = new AndroidBuilder(project.toString(), "created by Android Maven Plugin",
//                new DefaultProcessExecutor(logger),
//                new MavenErrorReporter(logger, MavenErrorReporter.EvaluationMode.STANDARD),
//                logger,
//                false)

        String minSdkVersion = null;
        String targetSdkVersion = null;
        int versionCode;
        if (parsedUsesSdk != null) {
            minSdkVersion = parsedUsesSdk.getMinSdkVersion();
            targetSdkVersion = parsedUsesSdk.getTargetSdkVersion();
        }
        if (Boolean.TRUE.equals(parsedVersionCodeUpdateFromVersion)) {
            VersionGenerator gen = new VersionGenerator(parsedVersionDigits, parsedVersionNamingPattern);

            versionCode = gen.generate(parsedVersionName);
        } else {
            versionCode = parsedVersionCode;
        }
        List<AndroidDependency> manifestDependencies = new ArrayList<>();

        if (Boolean.TRUE.equals(parsedMergeLibraries)) {
            final Set<Artifact> allArtifacts = project.getDependencyArtifacts();
            Set<Artifact> dependencyArtifacts = getArtifactResolverHelper().getFilteredArtifacts(allArtifacts);

            for (Artifact dependency : dependencyArtifacts) {
                final File unpackedLibFolder = getUnpackedLibFolder(dependency);
                final File manifestFile = new File(unpackedLibFolder, SdkConstants.FN_ANDROID_MANIFEST_XML);
                if (manifestFile.exists()) {
                    /*
                    File artifactFile,
                    MavenCoordinates coordinates,
                     String name,
                     String projectPath,
                     File extractedFolder
                    * */
                    // TODO this might not be working just yet....
                    manifestDependencies.add(AndroidDependency.createExplodedAarLibrary(
                            manifestFile,
                            null,
                            dependency.getArtifactId(),
                            unpackedLibFolder.getPath(),
                            unpackedLibFolder));
                }
            }
        }

        mergeManifestsForApplication(
                androidManifestFile,     // mainManifest
                new ArrayList<>(),   // manifestOverlays
                manifestDependencies,    // libraries
                "",                      // packageOverride
                versionCode,             // versionCode
                parsedVersionName,       // versionName
                minSdkVersion,           // minSdkVersion
                targetSdkVersion,        // targetSdkVersion
                null,                    // maxSdkVersion
                destinationManifestFile.getPath(),       // outManifestLocation
                null,                                    // outAaptSafeManifestLocation
                null,                                    // outInstantRunManifestLocation,
                ManifestMerger2.MergeType.APPLICATION,   // mergeType
                new HashMap<>(),           // placeHolders
                new ArrayList<>(),  // optionalFeatures
                parsedMergeReportFile,                    // reportFile
                logger
        );
    }

    /**
     * Merges the manifest of an application with the manifest of its dependencies.
     *
     * @param mainManifest                  The main manifest to merge with.
     * @param manifestOverlays              The List&lt;java.io.File&gt; of manifest overlays to merge the manifest with.
     * @param dependencies                  The list of dependencies to merge the manifest with.
     * @param packageOverride               The package to override in the manifest.
     * @param versionCode                   The version code to use when merging the manifest.
     * @param versionName                   The version name to use when merging the manifest.
     * @param minSdkVersion                 The minimum SDK version to use when merging the manifest.
     * @param targetSdkVersion              The target SDK version to use when merging the manifest.
     * @param maxSdkVersion                 The maximum SDK version to use when merging the manifest.
     * @param outManifestLocation           The location to write the merged manifest to.
     * @param outAaptSafeManifestLocation   The location to write the merged manifest to.
     * @param outInstantRunManifestLocation The location to write the merged manifest to.
     * @param mergeType                     The type of merge to perform.
     * @param placeHolders                  The placeholders to use when merging the manifest.
     * @param optionalFeatures              The optional features to enable.
     * @param reportFile                    The file to write the merging report to.
     * @param mLogger                       The logger to use.
     */
    public void mergeManifestsForApplication(@NonNull java.io.File mainManifest,
                                             @NonNull java.util.List<java.io.File> manifestOverlays,
                                             @NonNull java.util.List<? extends ManifestProvider> dependencies,
                                             java.lang.String packageOverride,
                                             int versionCode,
                                             java.lang.String versionName,
                                             @Nullable java.lang.String minSdkVersion,
                                             @Nullable java.lang.String targetSdkVersion,
                                             @Nullable java.lang.Integer maxSdkVersion,
                                             @NonNull java.lang.String outManifestLocation,
                                             @Nullable java.lang.String outAaptSafeManifestLocation,
                                             @Nullable java.lang.String outInstantRunManifestLocation,
                                             ManifestMerger2.MergeType mergeType,
                                             java.util.Map<java.lang.String, java.lang.Object> placeHolders,
                                             @NonNull java.util.List<ManifestMerger2.Invoker.Feature> optionalFeatures,
                                             @Nullable java.io.File reportFile,
                                             ILogger mLogger) {
        try {
            ManifestMerger2.Invoker manifestMergerInvoker =
                    ManifestMerger2.newMerger(mainManifest, mLogger, mergeType)
                            .setPlaceHolderValues(placeHolders)
                            .addFlavorAndBuildTypeManifests(manifestOverlays.toArray(new File[manifestOverlays.size()]))
                            .addManifestProviders(dependencies)
                            .withFeatures(optionalFeatures.toArray(
                                    new ManifestMerger2.Invoker.Feature[0]))
                            .setMergeReportFile(reportFile);
            if (mergeType == ManifestMerger2.MergeType.APPLICATION) {
                manifestMergerInvoker.withFeatures(ManifestMerger2.Invoker.Feature.REMOVE_TOOLS_DECLARATIONS);
            }
            //noinspection VariableNotUsedInsideIf
            if (outAaptSafeManifestLocation != null) {
                manifestMergerInvoker.withFeatures(ManifestMerger2.Invoker.Feature.MAKE_AAPT_SAFE);
            }
            setInjectableValues(manifestMergerInvoker,
                    packageOverride, versionCode, versionName,
                    minSdkVersion, targetSdkVersion, maxSdkVersion);
            MergingReport mergingReport = manifestMergerInvoker.merge();
            mLogger.verbose("Merging result: %1$s", mergingReport.getResult());
            switch (mergingReport.getResult()) {
                case WARNING:
                    mergingReport.log(mLogger);
                    // fall through since these are just warnings.
                case SUCCESS:
                    XmlDocument xmlDocument = mergingReport.getMergedXmlDocument(
                            MergingReport.MergedManifestKind.MERGED);
                    String annotatedDocument = mergingReport.getMergedDocument(
                            MergingReport.MergedManifestKind.BLAME);
                    if (annotatedDocument != null) {
                        mLogger.verbose(annotatedDocument);
                    }
                    save(Objects.requireNonNull(xmlDocument), new File(outManifestLocation));
                    mLogger.verbose(MERGED_MANIFEST_SAVED + outManifestLocation);
                    if (outAaptSafeManifestLocation != null) {
                        save(Objects.requireNonNull(
                                        mergingReport.getMergedXmlDocument(MergingReport.MergedManifestKind.AAPT_SAFE)),
                                new File(outAaptSafeManifestLocation));
                    }
                    if (outInstantRunManifestLocation != null) {
                        XmlDocument instantRunMergedManifest = mergingReport.getMergedXmlDocument(
                                MergingReport.MergedManifestKind.INSTANT_RUN);
                        if (instantRunMergedManifest != null) {
                            save(instantRunMergedManifest, new File(outInstantRunManifestLocation));
                        }
                    }
                    break;
                case ERROR:
                    mergingReport.log(mLogger);
                    throw new RuntimeException(mergingReport.getReportString());
                default:
                    throw new RuntimeException(UNHANDLED_RESULT_TYPE
                            + mergingReport.getResult());
            }
        } catch (ManifestMerger2.MergeFailureException e) {
            // TODO: unacceptable.
            throw new RuntimeException(e);
        }
    }

    /**
     * Saves the {@link com.android.manifmerger.XmlDocument} to a file in UTF-8 encoding.
     *
     * @param xmlDocument xml document to save.
     * @param out         file to save to.
     */
    private void save(@NonNull XmlDocument xmlDocument, File out) {
        try {
            Files.write(xmlDocument.prettyPrint(), out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
