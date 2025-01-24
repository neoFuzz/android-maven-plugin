/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.builder.core;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.compiling.DependencyFileProcessor;
import com.android.builder.core.BuildToolsServiceLoader.BuildToolServiceLoader;
import com.android.builder.dependency.ManifestDependency;
import com.android.builder.dependency.SymbolFileProvider;
import com.android.builder.internal.ClassFieldImpl;
import com.android.builder.internal.SymbolLoader;
import com.android.builder.internal.SymbolWriter;
import com.android.builder.internal.TestManifestGenerator;
import com.android.builder.internal.compiler.*;
import com.android.builder.internal.packaging.Packager;
import com.android.builder.model.ClassField;
import com.android.builder.model.PackagingOptions;
import com.android.builder.model.SigningConfig;
import com.android.builder.model.SyncIssue;
import com.android.builder.packaging.DuplicateFileException;
import com.android.builder.packaging.PackagerException;
import com.android.builder.packaging.SealedPackageException;
import com.android.builder.packaging.SigningException;
import com.android.builder.sdk.SdkInfo;
import com.android.builder.sdk.TargetInfo;
import com.android.builder.signing.SignedJarBuilder;
import com.android.ide.common.internal.AaptCruncher;
import com.android.ide.common.internal.LoggedErrorException;
import com.android.ide.common.internal.PngCruncher;
import com.android.ide.common.process.*;
import com.android.ide.common.signing.CertificateInfo;
import com.android.ide.common.signing.KeystoreHelper;
import com.android.ide.common.signing.KeytoolException;
import com.android.jack.api.ConfigNotSupportedException;
import com.android.jack.api.JackProvider;
import com.android.jack.api.v01.*;
import com.android.jill.api.JillProvider;
import com.android.jill.api.v01.Api01TranslationTask;
import com.android.jill.api.v01.TranslationException;
import com.android.manifmerger.*;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.IAndroidTarget.OptionalLibrary;
import com.android.sdklib.repository.FullRevision;
import com.android.utils.ILogger;
import com.android.utils.Pair;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.google.common.io.Files;
import org.w3c.dom.Document;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.android.SdkConstants.*;
import static com.android.builder.core.BuilderConstants.ANDROID_WEAR;
import static com.android.builder.core.BuilderConstants.ANDROID_WEAR_MICRO_APK;
import static com.android.manifmerger.ManifestMerger2.Invoker;
import static com.google.common.base.Preconditions.*;

/**
 * This is the main builder class. It is given all the data to process the build (such as
 * {@link DefaultProductFlavor}s, {@link DefaultBuildType} and dependencies) and use them when doing specific
 * build steps.
 * <p>
 * To use:
 * create a builder with {@link #AndroidBuilder(String, String, ProcessExecutor, JavaProcessExecutor, ErrorReporter, ILogger, boolean)}
 * <p>
 * then build steps can be done with
 * {@link #mergeManifests(File, List, List, String, int, String, String, String, Integer, String, String, ManifestMerger2.MergeType, Map, File)}
 * {@link #processTestManifest(String, String, String, String, String, Boolean, Boolean, File, List, Map, File, File)}
 * {@link #processResources(AaptPackageProcessBuilder, boolean, ProcessOutputHandler)}
 * {@link #compileAllAidlFiles(List, File, File, List, DependencyFileProcessor, ProcessOutputHandler)}
 * {@link #convertByteCode(Collection, Collection, File, boolean, File, DexOptions, List, File, boolean, boolean, ProcessOutputHandler)}
 * {@link #packageApk(String, File, Collection, Collection, String, Collection, File, Set, boolean, SigningConfig, PackagingOptions, SignedJarBuilder.IZipEntryFilter, String)}
 * <p>
 * Java compilation is not handled but the builder provides the bootclasspath with
 * {@link #getBootClasspath()}.
 */
public class AndroidBuilder {

    /**
     * Static string message for dex options null.
     */
    public static final String DEX_OPTIONS_NOT_NULL = "dexOptions cannot be null.";
    /**
     * Static string for when manifest is merged
     */
    public static final String MERGED_MANIFEST_SAVED = "Merged manifest saved to ";
    /**
     * Static string message for unhandled result type.
     */
    public static final String UNHANDLED_RESULT_TYPE = "Unhandled result type : ";
    /**
     * Static string message for import folders.
     */
    public static final String IMPORT_FOLDERS_NULL = "importFolders cannot be null.";
    /**
     * Static string message for source output.
     */
    public static final String SOURCE_OUTPUT_NULL = "sourceOutputDir cannot be null.";
    /**
     * Minimum build tools revision required by this class.
     */
    private static final FullRevision MIN_BUILD_TOOLS_REV = new FullRevision(19, 1, 0);
    /**
     * A no-op dependency file processor.
     */
    private static final DependencyFileProcessor sNoOpDependencyFileProcessor = dependencyFile -> null;
    /**
     * The project ID.
     */
    @NonNull
    private final String mProjectId;
    /**
     * The logger to use
     */
    @NonNull
    private final ILogger mLogger;

    /**
     *
     */
    @NonNull
    private final ProcessExecutor mProcessExecutor;
    /**
     * The java process executor.
     */
    @NonNull
    private final JavaProcessExecutor mJavaProcessExecutor;
    /**
     * The error reporter.
     */
    @NonNull
    private final ErrorReporter mErrorReporter;

    /**
     * Whether external tools (dx and aapt) are launched in verbose mode.
     */
    private final boolean mVerboseExec;

    /**
     * The createdBy String for the apk manifest.
     */
    @Nullable
    private final String mCreatedBy;

    /**
     * The SDK info for the build.
     */
    private SdkInfo mSdkInfo;
    /**
     * The target info for the build.
     */
    private TargetInfo mTargetInfo;

    /**
     * The list of boot classpath jars.
     */
    private List<File> mBootClasspath;
    /**
     * The list of libraries to be included in the classpath.
     */
    @NonNull
    private List<LibraryRequest> mLibraryRequests = ImmutableList.of();

    /**
     * Creates an AndroidBuilder.
     * <p>
     * <var>verboseExec</var> is needed on top of the ILogger due to remote exec tools not being
     * able to output info and verbose messages separately.
     *
     * @param projectId           the project ID. Must not be null.
     * @param createdBy           the createdBy String for the apk manifest.
     * @param processExecutor     the process executor
     * @param javaProcessExecutor the java process executor
     * @param errorReporter       the error reporter
     * @param logger              the Logger
     * @param verboseExec         whether external tools are launched in verbose mode
     */
    public AndroidBuilder(
            @NonNull String projectId,
            @Nullable String createdBy,
            @NonNull ProcessExecutor processExecutor,
            @NonNull JavaProcessExecutor javaProcessExecutor,
            @NonNull ErrorReporter errorReporter,
            @NonNull ILogger logger,
            boolean verboseExec) {
        mProjectId = checkNotNull(projectId);
        mCreatedBy = createdBy;
        mProcessExecutor = checkNotNull(processExecutor);
        mJavaProcessExecutor = checkNotNull(javaProcessExecutor);
        mErrorReporter = checkNotNull(errorReporter);
        mLogger = checkNotNull(logger);
        mVerboseExec = verboseExec;
    }

    /**
     * @param name      the name of the library. Must not be null.
     * @param libraries the list of libraries to search in. Must not be null.
     * @return the {@link LibraryRequest} object for the given library name, or <code>null</code> if
     * not found
     */
    @Nullable
    private static LibraryRequest findMatchingLib(@NonNull String name, @NonNull List<LibraryRequest> libraries) {
        for (LibraryRequest library : libraries) {
            if (name.equals(library.getName())) {
                return library;
            }
        }

        return null;
    }

    /**
     * @param type  the type of the class field. Must not be null.
     * @param name  the name of the class field. Must not be null.
     * @param value the value of the class field. Must not be null.
     * @return a new {@link ClassField} instance
     */
    @NonNull
    public static ClassField createClassField(@NonNull String type, @NonNull String name, @NonNull String value) {
        return new ClassFieldImpl(type, name, value);
    }

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
     * Collect the list of libraries' manifest files.
     *
     * @param libraries declared dependencies
     * @return A list of files and names for the libraries' manifest files.
     */
    @NonNull
    private static File[] collectLibraries(List<? extends ManifestDependency> libraries) {
        // Builder to collect only File objects
        ImmutableList.Builder<File> fileListBuilder = ImmutableList.builder();

        if (libraries != null) {
            // Use the existing helper method to collect library details
            ImmutableList.Builder<Pair<String, File>> manifestFiles = ImmutableList.builder();
            collectLibraries(libraries, manifestFiles);

            // Extract File objects from the collected pairs
            for (Pair<String, File> pair : manifestFiles.build()) {
                fileListBuilder.add(pair.getSecond());
            }
        }

        // Convert the list of files to an array
        return fileListBuilder.build().toArray(new File[0]);
    }

    /**
     * recursively calculate the list of libraries to merge the manifests files from.
     *
     * @param libraries     the dependencies
     * @param manifestFiles list of files and names identifiers for the libraries' manifest files.
     */
    private static void collectLibraries(@NonNull List<? extends ManifestDependency> libraries,
                                         ImmutableList.Builder<Pair<String, File>> manifestFiles) {

        for (ManifestDependency library : libraries) {
            manifestFiles.add(Pair.of(library.getName(), library.getManifest()));
            List<? extends ManifestDependency> manifestDependencies = library
                    .getManifestDependencies();
            if (!manifestDependencies.isEmpty()) {
                collectLibraries(manifestDependencies, manifestFiles);
            }
        }
    }

    /**
     * @param testApplicationId     the application id to use for the test application.
     * @param minSdkVersion         the minSdkVersion to use. Can be null.
     * @param targetSdkVersion      the targetSdkVersion to use. Can be null.
     * @param testedApplicationId   the application id of the application being tested.
     * @param instrumentationRunner the instrumentation runner to use. Can be null.
     * @param handleProfiling       whether the test handle profiling.
     * @param functionalTest        whether this is a functional test.
     * @param outManifestLocation   the location of the output manifest file.
     */
    private static void generateTestManifest(
            @NonNull String testApplicationId,
            @Nullable String minSdkVersion,
            @Nullable String targetSdkVersion,
            @NonNull String testedApplicationId,
            @NonNull String instrumentationRunner,
            @NonNull Boolean handleProfiling,
            @NonNull Boolean functionalTest,
            @NonNull File outManifestLocation) {
        TestManifestGenerator generator = new TestManifestGenerator(
                outManifestLocation,
                testApplicationId,
                minSdkVersion,
                targetSdkVersion,
                testedApplicationId,
                instrumentationRunner,
                handleProfiling,
                functionalTest);
        try {
            generator.generate();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param minSdkVersion    the minSdkVersion to use.
     * @param targetSdkVersion the targetSdkVersion to use. Can be null.
     * @param manifestFile     the manifest file to update.
     * @throws IOException if the manifest file cannot be written.
     */
    public static void generateApkDataEntryInManifest(
            int minSdkVersion,
            int targetSdkVersion,
            @NonNull File manifestFile)
            throws IOException {

        StringBuilder content = new StringBuilder();
        content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
                .append("<manifest package=\"\" xmlns:android=\"http://schemas.android.com/apk/res/android\">\n")
                .append("            <uses-sdk android:minSdkVersion=\"")
                .append(minSdkVersion).append("\"");
        if (targetSdkVersion != -1) {
            content.append(" android:targetSdkVersion=\"").append(targetSdkVersion).append("\"");
        }
        content.append("/>\n");
        content.append("    <application>\n")
                .append("        <meta-data android:name=\"" + ANDROID_WEAR + "\"\n")
                .append("                   android:resource=\"@xml/" + ANDROID_WEAR_MICRO_APK)
                .append("\" />\n")
                .append("   </application>\n")
                .append("</manifest>\n");

        Files.write(content, manifestFile, Charsets.UTF_8);
    }

    /**
     * Converts the bytecode to Dalvik format
     *
     * @param inputFile            the input file
     * @param outFile              the output file or folder if multi-dex is enabled.
     * @param multiDex             whether multidex is enabled.
     * @param dexOptions           the dex options
     * @param buildToolInfo        the build tools info
     * @param verbose              verbose flag
     * @param processExecutor      the java process executor
     * @param processOutputHandler the process output handler
     * @return the list of generated files.
     * @throws ProcessException if dexing fails
     */
    @NonNull
    public static ImmutableList<File> preDexLibrary(
            @NonNull File inputFile,
            @NonNull File outFile,
            boolean multiDex,
            @NonNull DexOptions dexOptions,
            @NonNull BuildToolInfo buildToolInfo,
            boolean verbose,
            @NonNull JavaProcessExecutor processExecutor,
            @NonNull ProcessOutputHandler processOutputHandler)
            throws ProcessException {
        checkNotNull(inputFile, "inputFile cannot be null.");
        checkNotNull(outFile, "outFile cannot be null.");
        checkNotNull(dexOptions, DEX_OPTIONS_NOT_NULL);


        try {
            if (!checkLibraryClassesJar(inputFile)) {
                return ImmutableList.of();
            }
        } catch (IOException e) {
            throw new RuntimeException("Exception while checking library jar", e);
        }
        DexProcessBuilder builder = new DexProcessBuilder(outFile);

        builder.setVerbose(verbose)
                .setMultiDex(multiDex)
                .addInput(inputFile);

        JavaProcessInfo javaProcessInfo = builder.build(buildToolInfo, dexOptions);

        ProcessResult result = processExecutor.execute(javaProcessInfo, processOutputHandler);
        result.rethrowFailure().assertNormalExitValue();

        if (multiDex) {
            File[] files = outFile.listFiles((file, name) -> name.endsWith(DOT_DEX));

            if (files == null || files.length == 0) {
                throw new RuntimeException("No dex files created at " + outFile.getAbsolutePath());
            }

            return ImmutableList.copyOf(files);
        } else {
            return ImmutableList.of(outFile);
        }
    }

    /**
     * Returns true if the library (jar or folder) contains class files, false otherwise.
     *
     * @param input The library file (jar or folder) to check
     * @return True if the library contains class files, false otherwise
     */
    private static boolean checkLibraryClassesJar(@NonNull File input) throws IOException {

        if (!input.exists()) {
            return false;
        }

        if (input.isDirectory()) {
            return checkFolder(input);
        }

        try (ZipFile zipFile = new ZipFile(input)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                if (entries.nextElement().getName().endsWith(".class")) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Returns true if this folder or one of its subfolder contains a class file, false otherwise.
     *
     * @param folder The folder to check for class files
     * @return true if a class file is found, false otherwise
     */
    private static boolean checkFolder(@NonNull File folder) {
        File[] subFolders = folder.listFiles();
        if (subFolders != null) {
            for (File childFolder : subFolders) {
                if (childFolder.isFile() && childFolder.getName().endsWith(".class")) {
                    return true;
                }
                if (childFolder.isDirectory() && checkFolder(childFolder)) {
                    return true;
                }

            }
        }
        return false;
    }

    /**
     * Converts a Java library file to Jack format using the Jill API if available.
     * If the API conversion fails or is not available, falls back to native conversion.
     *
     * @param inputFile            The Java library file to convert (typically a JAR or AAR file)
     * @param outFile              The destination file for the Jack format output
     * @param dexOptions           The dex options to be used during conversion
     * @param buildToolInfo        Information about the Android build tools
     * @param verbose              Whether to enable verbose output during conversion
     * @param processExecutor      The executor for running Java processes
     * @param processOutputHandler Handler for managing process output
     * @param logger               Logger instance for recording conversion messages and errors
     * @return A List containing the output file if conversion is successful
     * @throws ProcessException If an error occurs during the conversion process
     */
    public static List<File> convertLibaryToJackUsingApis(
            @NonNull File inputFile,
            @NonNull File outFile,
            @NonNull DexOptions dexOptions,
            @NonNull BuildToolInfo buildToolInfo,
            boolean verbose,
            @NonNull JavaProcessExecutor processExecutor,
            @NonNull ProcessOutputHandler processOutputHandler,
            @NonNull ILogger logger) throws ProcessException {

        BuildToolServiceLoader buildToolServiceLoader = BuildToolsServiceLoader.INSTANCE
                .forVersion(buildToolInfo);
        if (System.getenv("USE_JACK_API") != null) {
            try {
                Optional<JillProvider> jillProviderOptional = buildToolServiceLoader
                        .getSingleService(logger, BuildToolsServiceLoader.JILL);

                if (jillProviderOptional.isPresent()) {
                    com.android.jill.api.v01.Api01Config config =
                            jillProviderOptional.get().createConfig(
                                    com.android.jill.api.v01.Api01Config.class);

                    config.setInputJavaBinaryFile(inputFile);
                    config.setOutputJackFile(outFile);
                    config.setVerbose(verbose);

                    Api01TranslationTask translationTask = config.getTask();
                    translationTask.run();

                    return ImmutableList.of(outFile);
                }
            } catch (com.android.jill.api.ConfigNotSupportedException |
                     com.android.jill.api.v01.ConfigurationException e) {
                logger.warning(e.getMessage() + ", reverting to native");
            } catch (TranslationException e) {
                logger.error(e, "In process translation failed, reverting to native, file a bug");
            }
        }
        return convertLibraryToJack(inputFile, outFile, dexOptions, buildToolInfo, verbose,
                processExecutor, processOutputHandler, logger);
    }

    /**
     * @param inputFile            the input file
     * @param outFile              the output file or folder if multi-dex is enabled.
     * @param dexOptions           the dex options
     * @param buildToolInfo        the build tools info
     * @param verbose              verbose flag
     * @param processExecutor      the java process executor
     * @param processOutputHandler the process output handler
     * @param logger               The logger instance
     * @return the list of generated files.
     * @throws ProcessException if dexing fails
     */
    @NonNull
    public static List<File> convertLibraryToJack(
            @NonNull File inputFile,
            @NonNull File outFile,
            @NonNull DexOptions dexOptions,
            @NonNull BuildToolInfo buildToolInfo,
            boolean verbose,
            @NonNull JavaProcessExecutor processExecutor,
            @NonNull ProcessOutputHandler processOutputHandler,
            @NonNull ILogger logger)
            throws ProcessException {
        checkNotNull(inputFile, "inputFile cannot be null.");
        checkNotNull(outFile, "outFile cannot be null.");
        checkNotNull(dexOptions, DEX_OPTIONS_NOT_NULL);

        // launch dx: create the command line
        ProcessInfoBuilder builder = new ProcessInfoBuilder();

        String jill = buildToolInfo.getPath(BuildToolInfo.PathId.JILL);
        if (jill == null || !new File(jill).isFile()) {
            throw new IllegalStateException("jill.jar is missing");
        }

        builder.setClasspath(jill);
        builder.setMain("com.android.jill.Main");

        if (dexOptions.getJavaMaxHeapSize() != null) {
            builder.addJvmArg("-Xmx" + dexOptions.getJavaMaxHeapSize());
        }
        builder.addArgs(inputFile.getAbsolutePath());
        builder.addArgs("--output");
        builder.addArgs(outFile.getAbsolutePath());

        if (verbose) {
            builder.addArgs("--verbose");
        }

        logger.verbose(builder.toString());
        JavaProcessInfo javaProcessInfo = builder.createJavaProcess();
        ProcessResult result = processExecutor.execute(javaProcessInfo, processOutputHandler);
        result.rethrowFailure().assertNormalExitValue();

        return Collections.singletonList(outFile);
    }

    /**
     * Sets the SdkInfo and the targetInfo on the builder. This is required to actually
     * build (some of the steps).
     *
     * @param sdkInfo         the SdkInfo
     * @param targetInfo      the TargetInfo
     * @param libraryRequests the list of library requests, as obtained from the project.
     * @see com.android.builder.sdk.SdkLoader
     */
    public void setTargetInfo(
            @NonNull SdkInfo sdkInfo,
            @NonNull TargetInfo targetInfo,
            @NonNull Collection<LibraryRequest> libraryRequests) {
        mSdkInfo = sdkInfo;
        mTargetInfo = targetInfo;

        if (mTargetInfo.getBuildTools().getRevision().compareTo(MIN_BUILD_TOOLS_REV) < 0) {
            throw new IllegalArgumentException(String.format(
                    "The SDK Build Tools revision (%1$s) is too low for project '%2$s'. Minimum required is %3$s",
                    mTargetInfo.getBuildTools().getRevision(), mProjectId, MIN_BUILD_TOOLS_REV));
        }

        mLibraryRequests = ImmutableList.copyOf(libraryRequests);
    }

    /**
     * Returns the SdkInfo, if set.
     *
     * @return the SdkInfo, if set.
     */
    @Nullable
    public SdkInfo getSdkInfo() {
        return mSdkInfo;
    }

    /**
     * Returns the TargetInfo, if set.
     *
     * @return the TargetInfo, if set.
     */
    @Nullable
    public TargetInfo getTargetInfo() {
        return mTargetInfo;
    }

    /**
     * @return the {@link ILogger} to use for logging.
     */
    @NonNull
    public ILogger getLogger() {
        return mLogger;
    }

    /**
     * @return the {@link ErrorReporter} to use for reporting errors.
     */
    @NonNull
    public ErrorReporter getErrorReporter() {
        return mErrorReporter;
    }

    /**
     * Returns the compilation target, if set.
     *
     * @return the compilation target, or null if not set.
     */
    @Nullable
    public IAndroidTarget getTarget() {
        checkState(mTargetInfo != null,
                "Cannot call getTarget() before setTargetInfo() is called.");
        return mTargetInfo.getTarget();
    }

    /**
     * Returns whether the compilation target is a preview.
     *
     * @return true if the target is a preview, false otherwise.
     */
    public boolean isPreviewTarget() {
        checkState(mTargetInfo != null,
                "Cannot call isTargetAPreview() before setTargetInfo() is called.");
        return mTargetInfo.getTarget().getVersion().isPreview();
    }

    /**
     * @return the target codename, or null if not a preview target
     */
    public String getTargetCodename() {
        checkState(mTargetInfo != null,
                "Cannot call getTargetCodename() before setTargetInfo() is called.");
        return mTargetInfo.getTarget().getVersion().getCodename();
    }

    /**
     * @return the Dx jar file
     */
    @NonNull
    public File getDxJar() {
        checkState(mTargetInfo != null,
                "Cannot call getDxJar() before setTargetInfo() is called.");
        return new File(mTargetInfo.getBuildTools().getPath(BuildToolInfo.PathId.DX_JAR));
    }

    /**
     * Helper method to get the boot classpath to be used during compilation.
     *
     * @return the boot classpath as a list of Files
     */
    @NonNull
    public List<File> getBootClasspath() {
        if (mBootClasspath == null) {
            checkState(mTargetInfo != null,
                    "Cannot call getBootClasspath() before setTargetInfo() is called.");

            List<File> classpath = Lists.newArrayList();

            IAndroidTarget target = mTargetInfo.getTarget();

            for (String p : target.getBootClasspath()) {
                classpath.add(new File(p));
            }

            List<LibraryRequest> requestedLibs = Lists.newArrayList(mLibraryRequests);

            // add additional libraries if any
            List<OptionalLibrary> libs = target.getAdditionalLibraries();
            for (OptionalLibrary lib : libs) {
                // add it always for now
                classpath.add(lib.getJar());

                // remove from list of requested if match
                LibraryRequest requestedLib = findMatchingLib(lib.getName(), requestedLibs);
                if (requestedLib != null) {
                    requestedLibs.remove(requestedLib);
                }
            }

            // add optional libraries if needed.
            List<OptionalLibrary> optionalLibraries = target.getOptionalLibraries();
            for (OptionalLibrary lib : optionalLibraries) {
                // search if requested
                LibraryRequest requestedLib = findMatchingLib(lib.getName(), requestedLibs);
                if (requestedLib != null) {
                    // add to classpath
                    classpath.add(lib.getJar());

                    // remove from requested list.
                    requestedLibs.remove(requestedLib);
                }
            }

            // look for not found requested libraries.
            for (LibraryRequest library : requestedLibs) {
                mErrorReporter.handleSyncError(
                        library.getName(),
                        SyncIssue.TYPE_OPTIONAL_LIB_NOT_FOUND,
                        "Unable to find optional library: " + library.getName());
            }

            // add annotations.jar if needed.
            if (target.getVersion().getApiLevel() <= 15) {
                classpath.add(mSdkInfo.getAnnotationsJar());
            }

            mBootClasspath = ImmutableList.copyOf(classpath);
        }

        return mBootClasspath;
    }

    /**
     * Helper method to get the boot classpath to be used during compilation.
     *
     * @return the boot classpath as a list of Strings
     */
    @NonNull
    public List<String> getBootClasspathAsStrings() {
        List<File> classpath = getBootClasspath();

        // convert to Strings.
        List<String> results = Lists.newArrayListWithCapacity(classpath.size());
        for (File f : classpath) {
            results.add(f.getAbsolutePath());
        }

        return results;
    }

    /**
     * Returns the jar file for the renderscript mode.
     * <p>
     * This may return null if the SDK has not been loaded yet.
     *
     * @return the jar file, or null.
     * @see #setTargetInfo(SdkInfo, TargetInfo, Collection)
     */
    @Nullable
    public File getRenderScriptSupportJar() {
        if (mTargetInfo != null) {
            return RenderScriptProcessor.getSupportJar(
                    mTargetInfo.getBuildTools().getLocation().getAbsolutePath());
        }

        return null;
    }

    /**
     * Returns the {@code compile} classpath for this config. If the config tests a library, this
     * will include the classpath of the tested config.
     * <p>
     * If the SDK was loaded, this may include the renderscript support jar.
     *
     * @param variantConfiguration the VariantConfiguration to use
     * @return a non-null, but possibly empty set.
     */
    @NonNull
    public Set<File> getCompileClasspath(@NonNull VariantConfiguration<?, ?, ?> variantConfiguration) {
        Set<File> compileClasspath = variantConfiguration.getCompileClasspath();

        if (variantConfiguration.getRenderscriptSupportModeEnabled()) {
            File renderScriptSupportJar = getRenderScriptSupportJar();

            Set<File> fullJars = Sets.newHashSetWithExpectedSize(compileClasspath.size() + 1);
            fullJars.addAll(compileClasspath);
            if (renderScriptSupportJar != null) {
                fullJars.add(renderScriptSupportJar);
            }
            compileClasspath = fullJars;
        }

        return compileClasspath;
    }

    /**
     * Returns the list of packaged jars for this config. If the config tests a library, this
     * will include the jars of the tested config
     * <p>
     * If the SDK was loaded, this may include the renderscript support jar.
     *
     * @param variantConfiguration the VariantConfiguration to use
     * @return a non-null, but possibly empty list.
     */
    @NonNull
    public Set<File> getPackagedJars(@NonNull VariantConfiguration<?, ?, ?> variantConfiguration) {
        Set<File> packagedJars = Sets.newHashSet(variantConfiguration.getPackagedJars());

        if (variantConfiguration.getRenderscriptSupportModeEnabled()) {
            File renderScriptSupportJar = getRenderScriptSupportJar();

            if (renderScriptSupportJar != null) {
                packagedJars.add(renderScriptSupportJar);
            }
        }

        return packagedJars;
    }

    /**
     * Returns the native lib folder for the renderscript mode.
     * <p>
     * This may return null if the SDK has not been loaded yet.
     *
     * @return the folder, or null.
     * @see #setTargetInfo(SdkInfo, TargetInfo, Collection)
     */
    @Nullable
    public File getSupportNativeLibFolder() {
        if (mTargetInfo != null) {
            return RenderScriptProcessor.getSupportNativeLibFolder(
                    mTargetInfo.getBuildTools().getLocation().getAbsolutePath());
        }

        return null;
    }

    /**
     * Returns an {@link PngCruncher} using aapt underneath
     *
     * @param processOutputHandler an object to handle the executed process output.
     * @return an PngCruncher object
     */
    @NonNull
    public PngCruncher getAaptCruncher(ProcessOutputHandler processOutputHandler) {
        checkState(mTargetInfo != null,
                "Cannot call getAaptCruncher() before setTargetInfo() is called.");
        return new AaptCruncher(
                mTargetInfo.getBuildTools().getPath(BuildToolInfo.PathId.AAPT),
                mProcessExecutor,
                processOutputHandler);
    }

    /**
     * @return the process executor to use to execute external processes.
     */
    @NonNull
    public ProcessExecutor getProcessExecutor() {
        return mProcessExecutor;
    }

    /**
     * @param processInfo the process to execute.
     * @param handler     the handler for the process execution.
     * @return the result of the execution.
     */
    @NonNull
    public ProcessResult executeProcess(@NonNull ProcessInfo processInfo,
                                        @NonNull ProcessOutputHandler handler) {
        return mProcessExecutor.execute(processInfo, handler);
    }

    /**
     * Invoke the Manifest Merger version 2
     *
     * @param mainManifest                the main manifest file
     * @param manifestOverlays            the list of manifest overlay files
     * @param libraries                   the list of library dependencies
     * @param packageOverride             the package name override value
     * @param versionCode                 the version code value
     * @param versionName                 the version name value
     * @param minSdkVersion               the minimum SDK version
     * @param targetSdkVersion            the target SDK version
     * @param maxSdkVersion               the maximum SDK version
     * @param outManifestLocation         the output location for the merged manifest
     * @param outAaptSafeManifestLocation the output location for the aapt-safe merged manifest
     * @param mergeType                   the merge type to use
     * @param placeHolders                a map of placeholders to be substituted in the manifest. May be null.
     * @param reportFile                  the file to write the XML merge blame report to. May be null.
     */
    public void mergeManifests(
            @NonNull File mainManifest,
            @NonNull List<File> manifestOverlays,
            @NonNull List<? extends ManifestDependency> libraries,
            String packageOverride,
            int versionCode,
            String versionName,
            @Nullable String minSdkVersion,
            @Nullable String targetSdkVersion,
            @Nullable Integer maxSdkVersion,
            @NonNull String outManifestLocation,
            @Nullable String outAaptSafeManifestLocation,
            ManifestMerger2.MergeType mergeType,
            Map<String, String> placeHolders,
            @Nullable File reportFile) {

        try {
            Invoker manifestMergerInvoker =
                    ManifestMerger2.newMerger(mainManifest, mLogger, mergeType)
                            .setPlaceHolderValues(placeHolders)
                            .addFlavorAndBuildTypeManifests(
                                    manifestOverlays.toArray(new File[manifestOverlays.size()]))
                            .addLibraryManifests(collectLibraries(libraries))
                            .setMergeReportFile(reportFile);

            if (mergeType == ManifestMerger2.MergeType.APPLICATION) {
                manifestMergerInvoker.withFeatures(Invoker.Feature.REMOVE_TOOLS_DECLARATIONS);
            }

            setInjectableValues(manifestMergerInvoker,
                    packageOverride, versionCode, versionName,
                    minSdkVersion, targetSdkVersion, maxSdkVersion);

            MergingReport mergingReport = manifestMergerInvoker.merge();
            mLogger.info("Merging result:" + mergingReport.getResult());
            switch (mergingReport.getResult()) {
                case WARNING:
                    mergingReport.log(mLogger);
                    // fall through since these are just warnings.
                case SUCCESS:
                    XmlDocument xmlDocument = mergingReport.getMergedXmlDocument(MergingReport.MergedManifestKind.MERGED);
                    try {
                        String annotatedDocument = mergingReport.getActions().blame(xmlDocument);
                        mLogger.verbose(annotatedDocument);
                    } catch (Exception e) {
                        mLogger.error(e, "cannot print resulting xml");
                    }
                    save(xmlDocument, new File(outManifestLocation));
                    if (outAaptSafeManifestLocation != null) {
                        PlaceholderEncoder.visit((Document) xmlDocument);
                        save(xmlDocument, new File(outAaptSafeManifestLocation));
                    }
                    mLogger.info(MERGED_MANIFEST_SAVED + outManifestLocation);
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

    /**
     * Creates the manifest for a test variant
     *
     * @param testApplicationId     the application id of the test application
     * @param minSdkVersion         the minSdkVersion of the test application
     * @param targetSdkVersion      the targetSdkVersion of the test application
     * @param testedApplicationId   the application id of the tested application
     * @param instrumentationRunner the name of the instrumentation runner
     * @param handleProfiling       whether the Instrumentation object will turn profiling on and off
     * @param functionalTest        whether the Instrumentation class should run as a functional test
     * @param testManifestFile      optionally user provided AndroidManifest.xml for testing application
     * @param libraries             the library dependency graph
     * @param outManifest           the output location for the merged manifest
     * @param manifestPlaceholders  a map of placeholders to be substituted in the manifest. May be null.
     * @param tmpDir                a temporary directory to use for intermediate files
     * @see VariantConfiguration#getApplicationId()
     * @see VariantConfiguration#getTestedConfig()
     * @see VariantConfiguration#getMinSdkVersion()
     * @see VariantConfiguration#getTestedApplicationId()
     * @see VariantConfiguration#getInstrumentationRunner()
     * @see VariantConfiguration#getHandleProfiling()
     * @see VariantConfiguration#getFunctionalTest()
     * @see VariantConfiguration#getDirectLibraries()
     */
    public void processTestManifest(
            @NonNull String testApplicationId,
            @Nullable String minSdkVersion,
            @Nullable String targetSdkVersion,
            @NonNull String testedApplicationId,
            @NonNull String instrumentationRunner,
            @NonNull Boolean handleProfiling,
            @NonNull Boolean functionalTest,
            @Nullable File testManifestFile,
            @NonNull List<? extends ManifestDependency> libraries,
            @NonNull Map<String, Object> manifestPlaceholders,
            @NonNull File outManifest,
            @NonNull File tmpDir) {
        checkNotNull(testApplicationId, "testApplicationId cannot be null.");
        checkNotNull(testedApplicationId, "testedApplicationId cannot be null.");
        checkNotNull(instrumentationRunner, "instrumentationRunner cannot be null.");
        checkNotNull(handleProfiling, "handleProfiling cannot be null.");
        checkNotNull(functionalTest, "functionalTest cannot be null.");
        checkNotNull(libraries, "libraries cannot be null.");
        checkNotNull(outManifest, "outManifestLocation cannot be null.");

        try {
            tmpDir.mkdirs();
            File generatedTestManifest = libraries.isEmpty() && testManifestFile == null
                    ? outManifest : File.createTempFile("manifestMerger", ".xml", tmpDir);

            mLogger.verbose("Generating in %1$s", generatedTestManifest.getAbsolutePath());
            generateTestManifest(
                    testApplicationId,
                    minSdkVersion,
                    targetSdkVersion.equals("-1") ? null : targetSdkVersion,
                    testedApplicationId,
                    instrumentationRunner,
                    handleProfiling,
                    functionalTest,
                    generatedTestManifest);

            if (testManifestFile != null) {
                File mergedTestManifest = File.createTempFile("manifestMerger", ".xml", tmpDir);
                mLogger.verbose("Merging user supplied manifest in %1$s",
                        generatedTestManifest.getAbsolutePath());
                Invoker invoker = ManifestMerger2.newMerger(
                                testManifestFile, mLogger, ManifestMerger2.MergeType.APPLICATION)
                        .setOverride(ManifestSystemProperty.PACKAGE, testApplicationId)
                        .setPlaceHolderValues(manifestPlaceholders)
                        .setPlaceHolderValue(PlaceholderHandler.INSTRUMENTATION_RUNNER,
                                instrumentationRunner)
                        .addLibraryManifests(generatedTestManifest);
                if (minSdkVersion != null) {
                    invoker.setOverride(ManifestSystemProperty.MIN_SDK_VERSION, minSdkVersion);
                }
                if (!targetSdkVersion.equals("-1")) {
                    invoker.setOverride(ManifestSystemProperty.TARGET_SDK_VERSION, targetSdkVersion);
                }
                MergingReport mergingReport = invoker.merge();
                if (libraries.isEmpty()) {
                    handleMergingResult(mergingReport, outManifest);
                } else {
                    handleMergingResult(mergingReport, mergedTestManifest);
                    generatedTestManifest = mergedTestManifest;
                }
            }

            if (!libraries.isEmpty()) {
                MergingReport mergingReport = ManifestMerger2.newMerger(
                                generatedTestManifest, mLogger, ManifestMerger2.MergeType.APPLICATION)
                        .withFeatures(Invoker.Feature.REMOVE_TOOLS_DECLARATIONS)
                        .setOverride(ManifestSystemProperty.PACKAGE, testApplicationId)
                        .addLibraryManifests(collectLibraries(libraries))
                        .setPlaceHolderValues(manifestPlaceholders)
                        .merge();

                handleMergingResult(mergingReport, outManifest);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param mergingReport the merging report
     * @param outFile       the output file to save the merged manifest to
     */
    private void handleMergingResult(@NonNull MergingReport mergingReport, @NonNull File outFile) {
        switch (mergingReport.getResult()) {
            case WARNING:
                mergingReport.log(mLogger);
                // fall through since these are just warnings.
            case SUCCESS:
                XmlDocument xmlDocument = mergingReport.getMergedXmlDocument(MergingReport.MergedManifestKind.MERGED);
                try {
                    String annotatedDocument = mergingReport.getActions().blame(xmlDocument);
                    mLogger.verbose(annotatedDocument);
                } catch (Exception e) {
                    mLogger.error(e, "cannot print resulting xml");
                }
                save(xmlDocument, outFile);
                mLogger.info(MERGED_MANIFEST_SAVED + outFile);
                break;
            case ERROR:
                mergingReport.log(mLogger);
                throw new RuntimeException(mergingReport.getReportString());
            default:
                throw new RuntimeException(UNHANDLED_RESULT_TYPE
                        + mergingReport.getResult());
        }
    }

    /**
     * Process the resources and generate R.java and/or the packaged resources.
     *
     * @param aaptCommand              aapt command invocation parameters.
     * @param enforceUniquePackageName if true method will fail if some libraries share the same
     *                                 package name
     * @param processOutputHandler     an object to handle the executed process output
     * @throws IllegalStateException if {@code setTargetInfo()} was not
     *                               called prior to this method.
     * @throws IOException           if failed to write the output files
     * @throws ProcessException      if failed to execute aapt
     */
    public void processResources(
            @NonNull AaptPackageProcessBuilder aaptCommand,
            boolean enforceUniquePackageName,
            @NonNull ProcessOutputHandler processOutputHandler)
            throws IOException, ProcessException {

        checkState(mTargetInfo != null,
                "Cannot call processResources() before setTargetInfo() is called.");

        // launch aapt: create the command line
        ProcessInfo processInfo = aaptCommand.build(
                mTargetInfo.getBuildTools(), mTargetInfo.getTarget(), mLogger);

        ProcessResult result = mProcessExecutor.execute(processInfo, processOutputHandler);
        result.rethrowFailure().assertNormalExitValue();

        // now if the project has libraries, R needs to be created for each library,
        // but only if the current project is not a library.
        if (aaptCommand.getSourceOutputDir() != null
                && aaptCommand.getType() != VariantType.LIBRARY
                && !aaptCommand.getLibraries().isEmpty()) {
            SymbolLoader fullSymbolValues = null;

            // First pass processing the libraries, collecting them by packageName,
            // and ignoring the ones that have the same package name as the application
            // (since that R class was already created).
            String appPackageName = aaptCommand.getPackageForR();
            if (appPackageName == null) {
                appPackageName = VariantConfiguration.getManifestPackage(aaptCommand.getManifestFile());
            }

            // list of all the symbol loaders per package names.
            Multimap<String, SymbolLoader> libMap = ArrayListMultimap.create();

            for (SymbolFileProvider lib : aaptCommand.getLibraries()) {
                if (lib.isOptional()) {
                    continue;
                }
                String packageName = VariantConfiguration.getManifestPackage(lib.getManifest());
                if (appPackageName == null) {
                    continue;
                }

                if (appPackageName.equals(packageName)) {
                    if (enforceUniquePackageName) {
                        String msg = String.format(
                                "Error: A library uses the same package as this project: %s",
                                packageName);
                        throw new RuntimeException(msg);
                    }

                    // ignore libraries that have the same package name as the app
                    continue;
                }

                File rFile = lib.getSymbolFile();
                // if the library has no resource, this file won't exist.
                if (rFile.isFile()) {

                    // load the full values if that's not already been done.
                    // Doing it lazily allow us to support the case where there's no
                    // resources anywhere.
                    if (fullSymbolValues == null) {
                        fullSymbolValues = new SymbolLoader(new File(aaptCommand.getSymbolOutputDir(), "R.txt"),
                                mLogger);
                        fullSymbolValues.load();
                    }

                    SymbolLoader libSymbols = new SymbolLoader(rFile, mLogger);
                    libSymbols.load();


                    // store these symbols by associating them with the package name.
                    libMap.put(packageName, libSymbols);
                }
            }

            // now loop on all the package name, merge all the symbols to write, and write them
            for (String packageName : libMap.keySet()) {
                Collection<SymbolLoader> symbols = libMap.get(packageName);

                if (enforceUniquePackageName && symbols.size() > 1) {
                    String msg = String.format(
                            """
                                    Error: more than one library with package name '%s'
                                    You can temporarily disable this error with android.enforceUniquePackageName=false
                                    However, this is temporary and will be enforced in 1.0
                                    """, packageName);
                    throw new RuntimeException(msg);
                }

                SymbolWriter writer = new SymbolWriter(aaptCommand.getSourceOutputDir(), packageName,
                        fullSymbolValues);
                for (SymbolLoader symbolLoader : symbols) {
                    writer.addSymbolsToWrite(symbolLoader);
                }
                writer.write();
            }
        }
    }

    /**
     * @param apkFile      the apk file to generate the data for
     * @param outResFolder the output folder for the resources
     * @param mainPkgName  the package name of the main app
     * @param resName      the resource name for the apk
     * @throws ProcessException if failed to execute aapt
     * @throws IOException      if failed to write the output files
     */
    public void generateApkData(
            @NonNull File apkFile,
            @NonNull File outResFolder,
            @NonNull String mainPkgName,
            @NonNull String resName) throws ProcessException, IOException {

        // need to run aapt to get apk information
        BuildToolInfo buildToolInfo = mTargetInfo.getBuildTools();

        String aapt = buildToolInfo.getPath(BuildToolInfo.PathId.AAPT);
        if (aapt == null) {
            throw new IllegalStateException(
                    "Unable to get aapt location from Build Tools " + buildToolInfo.getRevision());
        }

        ApkInfoParser parser = new ApkInfoParser(new File(aapt), mProcessExecutor);
        ApkInfoParser.ApkInfo apkInfo = parser.parseApk(apkFile);

        if (!apkInfo.getPackageName().equals(mainPkgName)) {
            throw new RuntimeException("The main and the micro apps do not have the same package name.");
        }

        String content = String.format(
                """
                        <?xml version="1.0" encoding="utf-8"?>
                        <wearableApp package="%1$s">
                            <versionCode>%2$s</versionCode>
                            <versionName>%3$s</versionName>
                            <rawPathResId>%4$s</rawPathResId>
                        </wearableApp>""",
                apkInfo.getPackageName(),
                apkInfo.getVersionCode(),
                apkInfo.getVersionName(),
                resName);

        // xml folder
        File resXmlFile = new File(outResFolder, FD_RES_XML);
        resXmlFile.mkdirs();

        Files.write(content,
                new File(resXmlFile, ANDROID_WEAR_MICRO_APK + DOT_XML),
                Charsets.UTF_8);
    }

    /**
     * Compiles all the aidl files found in the given source folders.
     *
     * @param sourceFolders           all the source folders to find files to compile
     * @param sourceOutputDir         the output dir in which to generate the source code
     * @param parcelableOutputDir     the output dir in which to generate the parcelable files
     * @param importFolders           import folders
     * @param dependencyFileProcessor the dependencyFileProcessor to record the dependencies
     *                                of the compilation.
     * @param processOutputHandler    an object to handle the executed process output
     * @throws ProcessException     if failed to execute the aidl compiler
     * @throws IOException          if failed to compile the files
     * @throws InterruptedException if the process is interrupted
     * @throws LoggedErrorException if the process failed
     */
    public void compileAllAidlFiles(@NonNull List<File> sourceFolders,
                                    @NonNull File sourceOutputDir,
                                    @Nullable File parcelableOutputDir,
                                    @NonNull List<File> importFolders,
                                    @Nullable DependencyFileProcessor dependencyFileProcessor,
                                    @NonNull ProcessOutputHandler processOutputHandler)
            throws IOException, InterruptedException, LoggedErrorException, ProcessException {
        checkNotNull(sourceFolders, "sourceFolders cannot be null.");
        checkNotNull(sourceOutputDir, SOURCE_OUTPUT_NULL);
        checkNotNull(importFolders, IMPORT_FOLDERS_NULL);
        checkState(mTargetInfo != null,
                "Cannot call compileAllAidlFiles() before setTargetInfo() is called.");

        IAndroidTarget target = mTargetInfo.getTarget();
        BuildToolInfo buildToolInfo = mTargetInfo.getBuildTools();

        String aidl = buildToolInfo.getPath(BuildToolInfo.PathId.AIDL);
        if (aidl == null || !new File(aidl).isFile()) {
            throw new IllegalStateException("aidl is missing");
        }

        List<File> fullImportList = Lists.newArrayListWithCapacity(
                sourceFolders.size() + importFolders.size());
        fullImportList.addAll(sourceFolders);
        fullImportList.addAll(importFolders);

        AidlProcessor processor = new AidlProcessor(
                aidl,
                target.getPath(IAndroidTarget.ANDROID_AIDL),
                fullImportList,
                sourceOutputDir,
                parcelableOutputDir,
                dependencyFileProcessor != null ?
                        dependencyFileProcessor : sNoOpDependencyFileProcessor,
                mProcessExecutor,
                processOutputHandler);

        SourceSearcher searcher = new SourceSearcher(sourceFolders, "aidl");
        searcher.setUseExecutor(true);
        searcher.search(processor);
    }

    /**
     * Compiles the given aidl file.
     *
     * @param sourceFolder            the source folder containing the aidl file
     * @param aidlFile                the AIDL file to compile
     * @param sourceOutputDir         the output dir in which to generate the source code
     * @param parcelableOutputDir     the output dir in which to generate the parcelable files
     * @param importFolders           all the import folders, including the source folders.
     * @param dependencyFileProcessor the dependencyFileProcessor to record the dependencies
     *                                of the compilation.
     * @param processOutputHandler    an object to handle the executed process output
     * @throws ProcessException if failed to execute the aidl compiler
     * @throws IOException      if failed to compile the files
     */
    public void compileAidlFile(@NonNull File sourceFolder,
                                @NonNull File aidlFile,
                                @NonNull File sourceOutputDir,
                                @Nullable File parcelableOutputDir,
                                @NonNull List<File> importFolders,
                                @Nullable DependencyFileProcessor dependencyFileProcessor,
                                @NonNull ProcessOutputHandler processOutputHandler)
            throws IOException, ProcessException {
        checkNotNull(aidlFile, "aidlFile cannot be null.");
        checkNotNull(sourceOutputDir, SOURCE_OUTPUT_NULL);
        checkNotNull(importFolders, IMPORT_FOLDERS_NULL);
        checkState(mTargetInfo != null,
                "Cannot call compileAidlFile() before setTargetInfo() is called.");

        IAndroidTarget target = mTargetInfo.getTarget();
        BuildToolInfo buildToolInfo = mTargetInfo.getBuildTools();

        String aidl = buildToolInfo.getPath(BuildToolInfo.PathId.AIDL);
        if (aidl == null || !new File(aidl).isFile()) {
            throw new IllegalStateException("aidl is missing");
        }

        AidlProcessor processor = new AidlProcessor(
                aidl,
                target.getPath(IAndroidTarget.ANDROID_AIDL),
                importFolders,
                sourceOutputDir,
                parcelableOutputDir,
                dependencyFileProcessor != null ?
                        dependencyFileProcessor : sNoOpDependencyFileProcessor,
                mProcessExecutor,
                processOutputHandler);

        processor.processFile(sourceFolder, aidlFile);
    }

    /**
     * Compiles all the renderscript files found in the given source folders.
     * <p>
     * Right now this is the only way to compile them as the renderscript compiler requires all
     * renderscript files to be passed for all compilation.
     * <p>
     * Therefore whenever a renderscript file or header changes, all must be recompiled.
     *
     * @param sourceFolders        all the source folders to find files to compile
     * @param importFolders        all the import folders.
     * @param sourceOutputDir      the output dir in which to generate the source code
     * @param resOutputDir         the output dir in which to generate the bitcode file
     * @param objOutputDir         the output dir in which to generate the obj file
     * @param libOutputDir         the output dir in which to generate the .so file
     * @param targetApi            the target api
     * @param debugBuild           whether the build is debug type
     * @param optimLevel           the optimization level
     * @param ndkMode              whether the build is in NDK mode
     * @param supportMode          support mode flag to generate .so files.
     * @param abiFilters           ABI filters in case of support mode
     * @param processOutputHandler an object to handle the executed process output
     * @throws InterruptedException if the process is interrupted
     * @throws ProcessException     if failed to execute the renderscript compiler
     * @throws LoggedErrorException if the process failed
     * @throws IOException          if failed to compile the files
     */
    public void compileAllRenderscriptFiles(@NonNull List<File> sourceFolders,
                                            @NonNull List<File> importFolders,
                                            @NonNull File sourceOutputDir,
                                            @NonNull File resOutputDir,
                                            @NonNull File objOutputDir,
                                            @NonNull File libOutputDir,
                                            int targetApi,
                                            boolean debugBuild,
                                            int optimLevel,
                                            boolean ndkMode,
                                            boolean supportMode,
                                            @Nullable Set<String> abiFilters,
                                            @NonNull ProcessOutputHandler processOutputHandler)
            throws InterruptedException, ProcessException, LoggedErrorException, IOException {
        checkNotNull(sourceFolders, "sourceFolders cannot be null.");
        checkNotNull(importFolders, IMPORT_FOLDERS_NULL);
        checkNotNull(sourceOutputDir, SOURCE_OUTPUT_NULL);
        checkNotNull(resOutputDir, "resOutputDir cannot be null.");
        checkState(mTargetInfo != null,
                "Cannot call compileAllRenderscriptFiles() before setTargetInfo() is called.");

        BuildToolInfo buildToolInfo = mTargetInfo.getBuildTools();

        String renderscript = buildToolInfo.getPath(BuildToolInfo.PathId.LLVM_RS_CC);
        if (renderscript == null || !new File(renderscript).isFile()) {
            throw new IllegalStateException("llvm-rs-cc is missing");
        }

        RenderScriptProcessor processor = new RenderScriptProcessor(
                sourceFolders,
                importFolders,
                sourceOutputDir,
                resOutputDir,
                objOutputDir,
                libOutputDir,
                buildToolInfo,
                targetApi,
                debugBuild,
                optimLevel,
                ndkMode,
                supportMode,
                abiFilters);
        processor.build(mProcessExecutor, processOutputHandler);
    }

    /**
     * Computes and returns the leaf folders based on a given file extension.
     * <p>
     * This looks through all the given root import folders, and recursively search for leaf
     * folders containing files matching the given extensions. All the leaf folders are gathered
     * and returned in the list.
     *
     * @param extension     the extension to search for.
     * @param importFolders an array of list of root folders.
     * @return a list of leaf folder, never null.
     */
    @NonNull
    public List<File> getLeafFolders(@NonNull String extension, List<File>... importFolders) {
        List<File> results = Lists.newArrayList();

        if (importFolders != null) {
            for (List<File> folders : importFolders) {
                SourceSearcher searcher = new SourceSearcher(folders, extension);
                searcher.setUseExecutor(false);
                LeafFolderGatherer processor = new LeafFolderGatherer();
                try {
                    searcher.search(processor);
                } catch (IOException | LoggedErrorException | ProcessException | InterruptedException e) {
                    // won't happen as we're not using the executor, and our processor
                    // doesn't throw those.
                }

                results.addAll(processor.getFolders());
            }
        }

        return results;
    }

    /**
     * Converts the bytecode to Dalvik format
     *
     * @param inputs               the input files
     * @param preDexedLibraries    the list of pre-dex'ed libraries
     * @param outDexFolder         the location of the output folder
     * @param multidex             whether to multi-dex
     * @param mainDexList          the main dex list file, or null if none provided
     * @param dexOptions           dex options
     * @param additionalParameters list of additional parameters to give to dx
     * @param tmpFolder            the temporary folder to use for dex-ing
     * @param incremental          true if it should attempt incremental dex if applicable
     * @param optimize             true if the dex should be optimized, false otherwise
     * @param processOutputHandler an object to handle the executed process output
     * @throws IOException      if any I/O error occurred while dex-ing
     * @throws ProcessException if the process failed to execute
     */
    public void convertByteCode(
            @NonNull Collection<File> inputs,
            @NonNull Collection<File> preDexedLibraries,
            @NonNull File outDexFolder,
            boolean multidex,
            @Nullable File mainDexList,
            @NonNull DexOptions dexOptions,
            @Nullable List<String> additionalParameters,
            @NonNull File tmpFolder,
            boolean incremental,
            boolean optimize,
            @NonNull ProcessOutputHandler processOutputHandler)
            throws IOException, ProcessException {
        checkNotNull(inputs, "inputs cannot be null.");
        checkNotNull(preDexedLibraries, "preDexedLibraries cannot be null.");
        checkNotNull(outDexFolder, "outDexFolder cannot be null.");
        checkNotNull(dexOptions, DEX_OPTIONS_NOT_NULL);
        checkNotNull(tmpFolder, "tmpFolder cannot be null");
        checkArgument(outDexFolder.isDirectory(), "outDexFolder must be a folder");
        checkArgument(tmpFolder.isDirectory(), "tmpFolder must be a folder");
        checkState(mTargetInfo != null,
                "Cannot call convertByteCode() before setTargetInfo() is called.");

        ImmutableList.Builder<File> verifiedInputs = ImmutableList.builder();
        for (File input : inputs) {
            if (checkLibraryClassesJar(input)) {
                verifiedInputs.add(input);
            }
        }

        BuildToolInfo buildToolInfo = mTargetInfo.getBuildTools();
        DexProcessBuilder builder = new DexProcessBuilder(outDexFolder);

        builder.setVerbose(mVerboseExec)
                .setIncremental(incremental)
                .setNoOptimize(!optimize)
                .setMultiDex(multidex)
                .setMainDexList(mainDexList)
                .addInputs(preDexedLibraries)
                .addInputs(verifiedInputs.build());

        if (additionalParameters != null) {
            builder.additionalParameters(additionalParameters);
        }

        JavaProcessInfo javaProcessInfo = builder.build(buildToolInfo, dexOptions);

        ProcessResult result = mJavaProcessExecutor.execute(javaProcessInfo, processOutputHandler);
        result.rethrowFailure().assertNormalExitValue();
    }

    /**
     * @param allClassesJarFile the path to the file containing the list of all classes.
     * @param jarOfRoots        the path to the file containing the list of all roots.
     * @return a set of classes to keep in the main dex file.
     * @throws ProcessException if an error occurs while running dx.
     */
    public Set<String> createMainDexList(
            @NonNull File allClassesJarFile,
            @NonNull File jarOfRoots) throws ProcessException {

        BuildToolInfo buildToolInfo = mTargetInfo.getBuildTools();
        ProcessInfoBuilder builder = new ProcessInfoBuilder();

        String dx = buildToolInfo.getPath(BuildToolInfo.PathId.DX_JAR);
        if (dx == null || !new File(dx).isFile()) {
            throw new IllegalStateException("dx.jar is missing");
        }

        builder.setClasspath(dx);
        builder.setMain("com.android.multidex.ClassReferenceListBuilder");

        builder.addArgs(jarOfRoots.getAbsolutePath());
        builder.addArgs(allClassesJarFile.getAbsolutePath());

        CachedProcessOutputHandler processOutputHandler = new CachedProcessOutputHandler();

        mJavaProcessExecutor.execute(builder.createJavaProcess(), processOutputHandler)
                .rethrowFailure()
                .assertNormalExitValue();

        String content = processOutputHandler.getProcessOutput().getStandardOutputAsString();

        return Sets.newHashSet(Splitter.on('\n').split(content));
    }

    /**
     * Converts the bytecode to Dalvik format
     *
     * @param inputFile            the input file
     * @param outFile              the output file or folder if multi-dex is enabled.
     * @param multiDex             whether multidex is enabled.
     * @param dexOptions           dex options
     * @param processOutputHandler an object to handle the executed process output
     * @throws IOException          if an I/O error occurs while creating the output file or folder.
     * @throws InterruptedException if the operation is interrupted
     * @throws ProcessException     if an error occurs while running dx.
     */
    public void preDexLibrary(
            @NonNull File inputFile,
            @NonNull File outFile,
            boolean multiDex,
            @NonNull DexOptions dexOptions,
            @NonNull ProcessOutputHandler processOutputHandler)
            throws IOException, InterruptedException, ProcessException {
        checkState(mTargetInfo != null,
                "Cannot call preDexLibrary() before setTargetInfo() is called.");

        BuildToolInfo buildToolInfo = mTargetInfo.getBuildTools();

        PreDexCache.getCache().preDexLibrary(
                inputFile,
                outFile,
                multiDex,
                dexOptions,
                buildToolInfo,
                mVerboseExec,
                mJavaProcessExecutor,
                processOutputHandler);
    }

    /**
     * Converts java source code into android byte codes using the jack integration APIs.
     * Jack will run in memory.
     *
     * @param dexOutputFolder     the output folder for the dex files
     * @param jackOutputFile      the output file for the jack files
     * @param classpath           the classpath
     * @param packagedLibraries   the packaged libraries
     * @param sourceFiles         the source files
     * @param proguardFiles       the proguard files
     * @param mappingFile         the mapping file
     * @param jarJarRulesFiles    the jar jar rules files
     * @param incrementalDir      the incremental directory
     * @param javaResourcesFolder the java resources folder
     * @param multiDex            whether multi-dex is enabled
     * @param minSdkVersion       the minimum sdk version
     * @return true if the conversion was successful, false otherwise
     */
    public boolean convertByteCodeUsingJackApis(
            @NonNull File dexOutputFolder,
            @NonNull File jackOutputFile,
            @NonNull Collection<File> classpath,
            @NonNull Collection<File> packagedLibraries,
            @NonNull Collection<File> sourceFiles,
            @Nullable Collection<File> proguardFiles,
            @Nullable File mappingFile,
            @NonNull Collection<File> jarJarRulesFiles,
            @Nullable File incrementalDir,
            @Nullable File javaResourcesFolder,
            boolean multiDex,
            int minSdkVersion) {

        BuildToolServiceLoader buildToolServiceLoader
                = BuildToolsServiceLoader.INSTANCE.forVersion(mTargetInfo.getBuildTools());

        Api01CompilationTask compilationTask = null;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            Optional<JackProvider> jackProvider = buildToolServiceLoader
                    .getSingleService(getLogger(), BuildToolsServiceLoader.JACK);
            if (jackProvider.isPresent()) {
                Api01Config config;

                // Get configuration object
                try {
                    config = jackProvider.get().createConfig(Api01Config.class);

                    config.setClasspath(new ArrayList<>(classpath));
                    config.setOutputDexDir(dexOutputFolder);
                    config.setOutputJackFile(jackOutputFile);
                    config.setImportedJackLibraryFiles(new ArrayList<>(packagedLibraries));

                    if (proguardFiles != null) {
                        config.setProguardConfigFiles(new ArrayList<>(proguardFiles));
                    }

                    if (!jarJarRulesFiles.isEmpty()) {
                        config.setJarJarConfigFiles(ImmutableList.copyOf(jarJarRulesFiles));
                    }

                    if (multiDex) {
                        if (minSdkVersion < BuildToolInfo.SDK_LEVEL_FOR_MULTIDEX_NATIVE_SUPPORT) {
                            config.setMultiDexKind(MultiDexKind.LEGACY);
                        } else {
                            config.setMultiDexKind(MultiDexKind.NATIVE);
                        }
                    }

                    config.setSourceEntries(new ArrayList<>(sourceFiles));
                    if (mappingFile != null) {
                        config.setProperty("jack.obfuscation.mapping.dump", "true");
                        config.setObfuscationMappingOutputFile(mappingFile);
                    }

                    config.setProperty("jack.import.resource.policy", "keep-first");

                    config.setReporter(ReporterKind.DEFAULT, outputStream);

                    // set the incremental dir if set and either already exists or can be created.
                    if (incrementalDir != null) {
                        if (!incrementalDir.exists() && !incrementalDir.mkdirs()) {
                            mLogger.warning("Cannot create %1$s directory, "
                                    + "jack incremental support disabled", incrementalDir);
                        }
                        if (incrementalDir.exists()) {
                            config.setIncrementalDir(incrementalDir);
                        }
                    }
                    if (javaResourcesFolder != null) {
                        ArrayList<File> folders = Lists.newArrayListWithExpectedSize(3);
                        folders.add(javaResourcesFolder);
                        config.setResourceDirs(folders);
                    }

                    compilationTask = config.getTask();
                } catch (ConfigNotSupportedException e1) {
                    mLogger.warning("Jack APIs v01 not supported");
                } catch (ConfigurationException e) {
                    mLogger.error(e,
                            "Jack APIs v01 configuration failed, reverting to native process");
                }
            }

            if (compilationTask == null) {
                return false;
            }

            // Run the compilation
            try {
                compilationTask.run();
                mLogger.info(outputStream.toString());
                return true;
            } catch (CompilationException | ConfigurationException e) {
                mLogger.error(e, outputStream.toString());
            } catch (UnrecoverableException e) {
                mLogger.error(e, "Something out of Jack control has happened: " + e.getMessage());
            }
        } catch (Exception e) {
            getLogger().warning("Cannot load Jack APIs v01 " + e.getMessage());
            getLogger().warning("Reverting to native process invocation");
        }
        return false;
    }

    /**
     * Converts the bytecode of a library to the jack format
     *
     * @param inputFile            the input file
     * @param outFile              the location of the output {@code classes.dex} file
     * @param dexOptions           dex options
     * @param processOutputHandler an object to handle the executed process output
     * @throws ProcessException     if the conversion fails
     * @throws IOException          if the conversion fails
     * @throws InterruptedException if the conversion fails
     */
    public void convertLibraryToJack(
            @NonNull File inputFile,
            @NonNull File outFile,
            @NonNull DexOptions dexOptions,
            @NonNull ProcessOutputHandler processOutputHandler)
            throws ProcessException, IOException, InterruptedException {
        checkState(mTargetInfo != null,
                "Cannot call preJackLibrary() before setTargetInfo() is called.");

        BuildToolInfo buildToolInfo = mTargetInfo.getBuildTools();

        JackConversionCache.getCache().convertLibrary(
                inputFile,
                outFile,
                dexOptions,
                buildToolInfo,
                mVerboseExec,
                mJavaProcessExecutor,
                processOutputHandler,
                mLogger);
    }

    /**
     * Packages the apk.
     *
     * @param androidResPkgLocation  the location of the packaged resource file
     * @param dexFolder              the folder with the dex file.
     * @param dexedLibraries         optional collection of additional dex files to put in the apk.
     * @param packagedJars           the jars that are packaged (libraries + jar dependencies)
     * @param javaResourcesLocation  the processed Java resource folder
     * @param jniLibsFolders         the folders containing jni shared libraries
     * @param mergingFolder          folder to contain files that are being merged
     * @param abiFilters             optional ABI filter
     * @param jniDebugBuild          whether the app should include jni debug data
     * @param signingConfig          the signing configuration
     * @param packagingOptions       the packaging options
     * @param packagingOptionsFilter the filter to be used for the packaging options
     * @param outApkLocation         location of the APK.
     * @throws DuplicateFileException if a duplicate file is found
     * @throws FileNotFoundException  if the store location was not found
     * @throws KeytoolException       if there is an issue with the keystore
     * @throws PackagerException      if there is an issue with the packager
     * @throws SigningException       when the key cannot be read from the keystore
     * @see VariantConfiguration#getPackagedJars()
     */
    public void packageApk(
            @NonNull String androidResPkgLocation,
            @Nullable File dexFolder,
            @NonNull Collection<File> dexedLibraries,
            @NonNull Collection<File> packagedJars,
            @Nullable String javaResourcesLocation,
            @Nullable Collection<File> jniLibsFolders,
            @NonNull File mergingFolder,
            @Nullable Set<String> abiFilters,
            boolean jniDebugBuild,
            @Nullable SigningConfig signingConfig,
            @Nullable PackagingOptions packagingOptions,
            @Nullable SignedJarBuilder.IZipEntryFilter packagingOptionsFilter,
            @NonNull String outApkLocation)
            throws DuplicateFileException, FileNotFoundException,
            KeytoolException, PackagerException, SigningException {
        checkNotNull(androidResPkgLocation, "androidResPkgLocation cannot be null.");
        checkNotNull(outApkLocation, "outApkLocation cannot be null.");

        CertificateInfo certificateInfo = null;
        if (signingConfig != null && signingConfig.isSigningReady()) {
            //noinspection ConstantConditions - isSigningReady() called above.
            certificateInfo = KeystoreHelper.getCertificateInfo(signingConfig.getStoreType(),
                    signingConfig.getStoreFile(), signingConfig.getStorePassword(),
                    signingConfig.getKeyPassword(), signingConfig.getKeyAlias());
            if (certificateInfo == null) {
                throw new SigningException("Failed to read key from keystore");
            }
        }

        try {
            Packager packager = new Packager(
                    outApkLocation, androidResPkgLocation, mergingFolder,
                    certificateInfo, mCreatedBy, packagingOptions, packagingOptionsFilter, mLogger);

            // add dex folder to the apk root.
            if (dexFolder != null) {
                if (!dexFolder.isDirectory()) {
                    throw new IllegalArgumentException("dexFolder must be a directory");
                }
                packager.addDexFiles(dexFolder, dexedLibraries);
            }

            packager.setJniDebugMode(jniDebugBuild);

            if (javaResourcesLocation != null && !packagedJars.isEmpty()) {
                throw new PackagerException("javaResourcesLocation and packagedJars both provided");
            }
            if (javaResourcesLocation != null || !packagedJars.isEmpty()) {
                packager.addResources(javaResourcesLocation != null
                        ? new File(javaResourcesLocation) : Iterables.getOnlyElement(packagedJars));
            }

            // also add resources from library projects and jars
            if (jniLibsFolders != null) {
                for (File jniFolder : jniLibsFolders) {
                    if (jniFolder.isDirectory()) {
                        packager.addNativeLibraries(jniFolder, abiFilters);
                    }
                }
            }

            packager.sealApk();
        } catch (SealedPackageException e) {
            // shouldn't happen since we control the package from start to end.
            throw new RuntimeException(e);
        }
    }

    /**
     * Signs a single jar file using the passed {@link SigningConfig}.
     *
     * @param in            the jar file to sign.
     * @param signingConfig the signing configuration
     * @param out           the file path for the signed jar.
     * @throws IOException                                        if an error occurred while reading or writing the files.
     * @throws KeytoolException                                   if there is an issue with the keystore.
     * @throws SigningException                                   if there is an issue with the key.
     * @throws NoSuchAlgorithmException                           if the algorithm used for signing is not available.
     * @throws SignedJarBuilder.IZipEntryFilter.ZipAbortException if the signing process is aborted.
     * @throws com.android.builder.signing.SigningException       if there is an issue with the key.
     */
    public void signApk(File in, SigningConfig signingConfig, File out)
            throws IOException, KeytoolException, SigningException, NoSuchAlgorithmException,
            SignedJarBuilder.IZipEntryFilter.ZipAbortException,
            com.android.builder.signing.SigningException {

        CertificateInfo certificateInfo = null;
        if (signingConfig != null && signingConfig.isSigningReady()) {
            //noinspection ConstantConditions - isSigningReady() called above.
            certificateInfo = KeystoreHelper.getCertificateInfo(signingConfig.getStoreType(),
                    signingConfig.getStoreFile(), signingConfig.getStorePassword(),
                    signingConfig.getKeyPassword(), signingConfig.getKeyAlias());
            if (certificateInfo == null) {
                throw new SigningException("Failed to read key from keystore");
            }
        }

        SignedJarBuilder signedJarBuilder = new SignedJarBuilder(
                new FileOutputStream(out),
                certificateInfo != null ? certificateInfo.getKey() : null,
                certificateInfo != null ? certificateInfo.getCertificate() : null,
                Packager.getLocalVersion(), mCreatedBy);


        signedJarBuilder.writeZip(new FileInputStream(in));
        signedJarBuilder.close();

    }

    /**
     * Merges the manifest of an application with the manifest of its dependencies.
     *
     * @param mainManifest                  The main manifest to merge with.
     * @param manifestOverlays              The list of manifest overlays to merge the manifest with.
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
     * @param placeHolders                  The place holders to use when merging the manifest.
     * @param optionalFeatures              The optional features to enable.
     * @param reportFile                    The file to write the merging report to.
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
                                             @NonNull java.util.List<Invoker.Feature> optionalFeatures,
                                             @Nullable java.io.File reportFile) {
        try {
            Invoker manifestMergerInvoker =
                    ManifestMerger2.newMerger(mainManifest, mLogger, mergeType)
                            .setPlaceHolderValues(placeHolders)
                            .addFlavorAndBuildTypeManifests(manifestOverlays.toArray(new File[manifestOverlays.size()]))
                            .addManifestProviders(dependencies)
                            .withFeatures(optionalFeatures.toArray(
                                    new Invoker.Feature[0]))
                            .setMergeReportFile(reportFile);
            if (mergeType == ManifestMerger2.MergeType.APPLICATION) {
                manifestMergerInvoker.withFeatures(Invoker.Feature.REMOVE_TOOLS_DECLARATIONS);
            }
            //noinspection VariableNotUsedInsideIf
            if (outAaptSafeManifestLocation != null) {
                manifestMergerInvoker.withFeatures(Invoker.Feature.MAKE_AAPT_SAFE);
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
}
