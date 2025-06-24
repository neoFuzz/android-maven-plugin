/*
 * Copyright (C) 2009 Jayway AB
 * Copyright (C) 2007-2008 JVending Masa
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
package com.github.cardforge.maven.plugins.android.phase09package;

import com.android.sdklib.build.ApkBuilder;
import com.android.sdklib.build.ApkCreationException;
import com.android.sdklib.build.DuplicateFileException;
import com.android.sdklib.build.SealedApkException;
import com.github.cardforge.maven.plugins.android.*;
import com.github.cardforge.maven.plugins.android.common.AaptCommandBuilder;
import com.github.cardforge.maven.plugins.android.common.NativeHelper;
import com.github.cardforge.maven.plugins.android.config.ConfigHandler;
import com.github.cardforge.maven.plugins.android.config.ConfigPojo;
import com.github.cardforge.maven.plugins.android.config.PullParameter;
import com.github.cardforge.maven.plugins.android.configuration.Apk;
import com.github.cardforge.maven.plugins.android.configuration.MetaInf;
import com.github.cardforge.maven.plugins.android.configuration.Sign;
import com.google.common.graph.Traverser;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.shade.resource.ReproducibleResourceTransformer;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.*;
import java.util.*;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static com.github.cardforge.maven.plugins.android.InclusionExclusionResolver.filterArtifacts;
import static com.github.cardforge.maven.plugins.android.common.AndroidExtension.AAR;
import static com.github.cardforge.maven.plugins.android.common.AndroidExtension.APK;

/**
 * Creates the apk file. By default, signs it with debug keystore.<br>
 * Change that by setting configuration parameter
 * <code>&lt;sign&gt;&lt;debug&gt;false&lt;/debug&gt;&lt;/sign&gt;</code>.
 *
 * @author hugo.josefson@jayway.com
 */
@SuppressWarnings("unused")
@Mojo(name = "apk",
        defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class ApkMojo extends AbstractAndroidMojo {

    private static final Pattern PATTERN_JAR_EXT = Pattern.compile("^.+\\.jar$", Pattern.CASE_INSENSITIVE);
    private static final String DEX_SUFFIX = ".dex";
    private static final String CLASSES = "classes";
    private final Map<String, List<File>> jars = new HashMap<>();
    /**
     * <p>How to sign the apk.</p>
     * <p>Looks like this:</p>
     * <pre>
     * &lt;sign&gt;
     *     &lt;debug&gt;auto&lt;/debug&gt;
     * &lt;/sign&gt;
     * </pre>
     * <p>Valid values for <code>&lt;debug&gt;</code> are:
     * <ul>
     * <li><code>true</code> = sign with the debug keystore.
     * <li><code>false</code> = don't sign with the debug keystore.
     * <li><code>both</code> = create a signed as well as an unsigned apk.
     * <li><code>auto</code> (default) = sign with debug keystore, unless another keystore is defined. (Signing with
     * other keystores is not yet implemented. See
     * <a href="http://code.google.com/p/maven-android-plugin/issues/detail?id=2">Issue 2</a>.)
     * </ul></p>
     * <p>Can also be configured from command-line with parameter <code>-Dandroid.sign.debug</code>.</p>
     */
    @Parameter
    private Sign sign;
    /**
     * <p>Parameter designed to pick up <code>-Dandroid.sign.debug</code> in case there is no pom with a
     * <code>&lt;sign&gt;</code> configuration tag.</p>
     * <p>Corresponds to {@link Sign#getDebug()}.</p>
     */
    @Parameter(property = "android.sign.debug", defaultValue = "auto", readonly = true)
    private String signDebug;
    /**
     * Rewrite the manifest so that all of its instrumentation components target the given package.
     * This value will be passed on to the aapt parameter --rename-instrumentation-target-package.
     * Look to aapt for more help on this.
     */
    @Parameter(property = "android.renameInstrumentationTargetPackage")
    private String renameInstrumentationTargetPackage; // is this passed into AaptExecutor? it appears to be
    /**
     * <p>Allows to detect and extract the duplicate files from embedded jars. In that case, the plugin analyzes
     * the content of all embedded dependencies and checks they are no duplicates inside those dependencies. Indeed,
     * Android does not support duplicates, and all dependencies are inlined in the APK. If duplicates files are found,
     * the resource is kept in the first dependency and removes from others.
     */
    @Parameter(property = "android.extractDuplicates", defaultValue = "false")
    private boolean extractDuplicates;
    /**
     * <p>Classifier to add to the artifact generated. If given, the artifact will be an attachment instead.</p>
     */
    @Parameter
    private String classifier;
    /**
     * The apk file produced by the apk goal. Per default the file is placed into the build directory (target
     * normally) using the build final name and apk as extension.
     */
    @Parameter(property = "android.outputApk",
            defaultValue = "${project.build.directory}/${project.build.finalName}.apk")
    private String outputApk;
    /**
     * <p>Additional source directories that contain resources to be packaged into the apk.</p>
     * <p>These are not source directories, that contain java classes to be compiled.
     * It corresponds to the -df option of the apkbuilder program. It allows you to specify directories,
     * that contain additional resources to be packaged into the apk. </p>
     * So an example inside the plugin configuration could be:
     * <pre>
     * &lt;configuration&gt;
     *   ...
     *    &lt;sourceDirectories&gt;
     *      &lt;sourceDirectory&gt;${project.basedir}/additionals&lt;/sourceDirectory&gt;
     *   &lt;/sourceDirectories&gt;
     *   ...
     * &lt;/configuration&gt;
     * </pre>
     */
    @Parameter(property = "android.sourceDirectories")
    private File[] sourceDirectories;
    @Parameter(alias = "metaInf")
    private MetaInf pluginMetaInf;
    /**
     * Defines whether the APK is being produced in debug mode or not.
     */
    @Parameter(property = "android.apk.debug")
    @PullParameter(defaultValue = "false")
    private Boolean apkDebug;
    @Parameter(property = "android.nativeToolchain")
    @PullParameter(defaultValue = "arm-linux-androideabi-4.4.3")
    private String apkNativeToolchain;
    /**
     * Specifies the final name of the library output by the build (this allows
     */
    @Parameter(property = "android.ndk.build.build.final-library.name")
    private String ndkFinalLibraryName;
    /**
     * Specify a list of patterns that are matched against the names of jar file
     * dependencies. Matching jar files will not have their resources added to the
     * resulting APK.
     * <p>
     * The patterns are standard Java regexes.
     */
    @Parameter
    private String[] excludeJarResources;
    private Pattern[] excludeJarResourcesPatterns;
    /**
     * Embedded configuration of this mojo.
     */
    @Parameter
    @ConfigPojo(prefix = "apk")
    private Apk apk;
    /**
     * Skips transitive dependencies. May be useful if the target classes directory is populated with the
     * {@code maven-dependency-plugin} and already contains all dependency classes.
     */
    @Parameter(property = "skipDependencies", defaultValue = "false")
    private boolean skipDependencies;
    /**
     * Allows to include or exclude artifacts by type. The {@code include} parameter has higher priority than the
     * {@code exclude} parameter. These two parameters can be overridden by the {@code artifactSet} parameter. Empty
     * strings are ignored. Example:
     * <pre>
     *     &lt;artifactTypeSet&gt;
     *         &lt;includes&gt;
     *             &lt;include&gt;aar&lt;/include&gt;
     *         &lt;includes&gt;
     *         &lt;excludes&gt;
     *             &lt;exclude&gt;jar&lt;/exclude&gt;
     *         &lt;excludes&gt;
     *     &lt;/artifactTypeSet&gt;
     * </pre>
     */
    @Parameter(property = "artifactTypeSet")
    private IncludeExcludeSet artifactTypeSet;
    /**
     * Allows to include or exclude artifacts by {@code groupId}, {@code artifactId}, and {@code versionId}. The
     * {@code include} parameter has higher priority than the {@code exclude} parameter. These two parameters can
     * override the {@code artifactTypeSet} and {@code skipDependencies} parameters. Artifact {@code groupId},
     * {@code artifactId}, and {@code versionId} are specified by a string with the respective values separated using
     * a colon character {@code :}. {@code artifactId} and {@code versionId} can be optional covering an artifact
     * range. Empty strings are ignored. Example:
     * <pre>
     *     &lt;artifactTypeSet&gt;
     *         &lt;includes&gt;
     *             &lt;include&gt;foo-group:foo-artifact:1.0-SNAPSHOT&lt;/include&gt;
     *             &lt;include&gt;bar-group:bar-artifact:1.0-SNAPSHOT&lt;/include&gt;
     *             &lt;include&gt;baz-group:*&lt;/include&gt;
     *         &lt;includes&gt;
     *         &lt;excludes&gt;
     *             &lt;exclude&gt;qux-group:qux-artifact:*&lt;/exclude&gt;
     *         &lt;excludes&gt;
     *     &lt;/artifactTypeSet&gt;
     * </pre>
     */
    @Parameter(property = "artifactSet")
    private IncludeExcludeSet artifactSet;
    /**
     * <p>Default hardware architecture for native library dependencies (with {@code &lt;type>so&lt;/type>})
     * without a classifier.</p>
     * <p>Valid values currently include {@code armeabi}, {@code armeabi-v7a}, {@code mips} and {@code x86}.</p>
     */
    @Parameter(property = "android.nativeLibrariesDependenciesHardwareArchitectureDefault", defaultValue = "armeabi")
    private String nativeLibrariesDependenciesHardwareArchitectureDefault;
    @Parameter
    private ReproducibleResourceTransformer[] transformers;

    /**
     * {@inheritDoc}
     */
    @Inject
    protected ApkMojo(ArtifactResolver artifactResolver, ArtifactHandler artHandler,
                      MavenProjectHelper projectHelper, DependencyGraphBuilder dependencyGraphBuilder) {
        super(artifactResolver, artHandler, projectHelper, dependencyGraphBuilder);
    }

    /**
     * Copies an input stream into an output stream but does not close the streams.
     *
     * @param in  the input stream
     * @param out the output stream
     * @throws IOException if the stream cannot be copied
     */
    private static void copyStreamWithoutClosing(@Nonnull InputStream in, OutputStream out) throws IOException {
        final int bufferSize = 4096;
        byte[] b = new byte[bufferSize];
        int n;
        while ((n = in.read(b)) != -1) {
            out.write(b, 0, n);
        }
    }

    /**
     * Executes the Mojo goal by performing the following tasks:
     * <ul>
     *     <li>Parses the configuration settings.</li>
     *     <li>If APK generation is enabled, calls {@code #execute()} to generate the APK.</li>
     * </ul>
     * <p>
     * This method is typically invoked during the execution of a Maven build to handle APK compilation and
     * artifact attachment tasks.
     * </p>
     *
     * @throws MojoExecutionException If there is an error executing the Mojo.
     * @throws MojoFailureException   If the Mojo fails due to a configuration or other critical issue.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {

        // Make an early exit if we're not supposed to generate the APK
        if (!generateApk) {
            return;
        }

        ConfigHandler cfh = new ConfigHandler(this, this.session, this.execution);

        cfh.parseConfiguration();

        generateIntermediateApk();

        // Compile resource exclusion patterns, if any
        if (excludeJarResources != null && excludeJarResources.length > 0) {
            getLog().debug("Compiling " + excludeJarResources.length + " patterns");

            excludeJarResourcesPatterns = new Pattern[excludeJarResources.length];

            for (int index = 0; index < excludeJarResources.length; ++index) {
                excludeJarResourcesPatterns[index] = Pattern.compile(excludeJarResources[index]);
            }
        }

        // Initialize apk build configuration
        File outputFile = new File(outputApk);
        final boolean signAsDebug = getAndroidSigner().isSignWithDebugKeyStore();

        if (getAndroidSigner().shouldCreateBothSignedAndUnsignedApk()) {
            getLog().info("Creating debug key signed apk file " + outputFile);
            createApkFile(outputFile, true);
            final File unsignedOutputFile = new File(targetDirectory,
                    finalName + "-unsigned." + APK);
            getLog().info("Creating additional unsigned apk file " + unsignedOutputFile);
            createApkFile(unsignedOutputFile, false);
            projectHelper.attachArtifact(project, unsignedOutputFile,
                    classifier == null ? "unsigned" : classifier + "_unsigned");
        } else {
            createApkFile(outputFile, signAsDebug);
        }

        if (classifier == null) {
            // Set the generated .apk file as the main artifact (because the pom states <packaging>apk</packaging>)
            project.getArtifact().setFile(outputFile);
        } else {
            // If there is a classifier specified, attach the artifact using that
            projectHelper.attachArtifact(project, APK, classifier, outputFile);
        }
    }

    void createApkFile(File outputFile, boolean signWithDebugKeyStore) throws MojoExecutionException {
        //this needs to come from DexMojo
        File dexFile = new File(targetDirectory, "classes.dex");
        if (!dexFile.exists()) {
            dexFile = new File(targetDirectory, "classes.zip");
        }

        File zipArchive = new File(targetDirectory, finalName + ".ap_");
        ArrayList<File> sourceFolders = new ArrayList<>();
        if (sourceDirectories != null) {
            sourceFolders.addAll(Arrays.asList(sourceDirectories));
        }
        ArrayList<File> jarFiles = new ArrayList<>();

        // Process the native libraries, looking both in the current build directory and
        // at the dependencies declared in the pom.  Currently, all .so files are automatically included
        final Collection<File> nativeFolders = getNativeLibraryFolders();
        getLog().info("Adding native libraries : " + nativeFolders);

        doAPKWithAPKBuilder(outputFile, dexFile, zipArchive, sourceFolders, jarFiles, nativeFolders,
                signWithDebugKeyStore);


    }

    private void computeDuplicateFiles(File jar) throws IOException {
        try (ZipFile file = new ZipFile(jar)) {  // try-with-resources
            Enumeration<? extends ZipEntry> list = file.entries();
            while (list.hasMoreElements()) {
                ZipEntry ze = list.nextElement();
                if (!(ze.getName().contains("META-INF/") || ze.isDirectory())) { // Exclude META-INF and Directories
                    List<File> l = jars.computeIfAbsent(ze.getName(), k -> new ArrayList<>());
                    l.add(jar);
                }
            }
        } // file will be automatically closed here
    }


    private void computeDuplicateFilesInSource(@Nonnull File folder) {
        String rPath = folder.getAbsolutePath();

        // Create a traverser for the file tree
        Traverser<File> traverser = Traverser.forTree(file -> {
            // Return the children of the current file (if it's a directory)
            if (file.isDirectory()) {
                return List.of(file.listFiles()); // list the contents of the directory
            } else {
                return List.of(); // leaf nodes (files) don't have children
            }
        });

        // Traverse the file tree breadth-first
        for (File file : traverser.breadthFirst(folder)) {
            String lPath = file.getAbsolutePath();

            // Skip the root directory
            if (lPath.equals(rPath)) {
                continue;
            }

            // Make the relative path
            lPath = lPath.substring(rPath.length() + 1);

            // Store files by relative path in the `jars` map
            jars.computeIfAbsent(lPath, k -> new ArrayList<>());
            jars.get(lPath).add(file);
        }
    }

    /**
     * Extracts duplicate files from the given JAR files and source folders and adds them to the APK builder.
     *
     * @param jarFiles      the list of JAR files to process
     * @param sourceFolders the list of source folders to process
     * @throws IOException if an I/O error occurs
     */
    private void extractDuplicateFiles(List<File> jarFiles, Collection<File> sourceFolders) throws IOException { // NOSONAR check on later
        getLog().debug("Extracting duplicates");
        List<String> duplicates = new ArrayList<>();
        List<File> jarToModify = new ArrayList<>();
        for (Map.Entry<String, List<File>> entry : jars.entrySet()) {
            List<File> files = entry.getValue();
            if (files.size() > 1) {
                String jarName = entry.getKey();
                getLog().warn("Duplicate file " + jarName + " : " + files);
                duplicates.add(jarName);

                // Add all non-duplicate files in one go using removeAll
                List<File> newFiles = new ArrayList<>(files);
                newFiles.removeAll(jarToModify);
                jarToModify.addAll(newFiles);
            }
        }

        // Rebuild jars.  Remove duplicates from ALL jars, then add them back into a duplicate-resources.jar
        File tmp = new File(targetDirectory.getAbsolutePath(), "unpacked-embedded-jars");
        boolean md = tmp.mkdirs();
        if (md) getLog().debug("Directory unpacked-embedded-jars created");
        File duplicatesJar = new File(tmp, "duplicate-resources.jar");
        Set<String> duplicatesAdded = new HashSet<>();
        boolean b = duplicatesJar.createNewFile();
        if (b) getLog().debug("File duplicate-resources.jar created");

        try (FileOutputStream fos = new FileOutputStream(duplicatesJar);
             JarOutputStream zos = new JarOutputStream(fos)) {
            findAndExtract(jarFiles, jarToModify, duplicates, duplicatesAdded, zos);
        }

        if (!jarToModify.isEmpty() && duplicatesJar.length() > 0) {
            jarFiles.add(duplicatesJar);
        }
    }

    /**
     * Finds and extracts duplicate files from the given JAR files and source folders, and adds them to the APK builder.
     *
     * @param jarFiles        the list of JAR files to process
     * @param jarToModify     the list of JAR files to modify
     * @param duplicates      the list of duplicate file names
     * @param duplicatesAdded the set of duplicate file names that have already been added
     * @param zos             the JarOutputStream to write the modified files to
     * @throws IOException if an I/O error occurs
     */
    private void findAndExtract(List<File> jarFiles, @Nonnull List<File> jarToModify, List<String> duplicates,
                                Set<String> duplicatesAdded, JarOutputStream zos)
            throws IOException {
        for (File file : jarToModify) {
            final int index = jarFiles.indexOf(file);
            if (index != -1) {
                final File newJar = removeDuplicatesFromJar(file, duplicates, duplicatesAdded, zos, index);
                getLog().debug("Removed duplicates from JAR " + newJar);
                if (newJar != null) {
                    jarFiles.set(index, newJar);
                }
            } else {
                removeDuplicatesFromFolder(file, file, duplicates, duplicatesAdded, zos);
                getLog().debug("Removed duplicates from FILE " + file);
            }
        }
        //add transformed resources to duplicate-resources.jar
        if (transformers != null) {
            for (ResourceTransformer transformer : transformers) {
                if (transformer.hasTransformedResource()) {
                    transformer.modifyOutputStream(zos);
                }
            }
        }
    }

    /**
     * Creates the APK file using the internal APKBuilder.
     *
     * @param outputFile            the output file
     * @param dexFile               the dex file
     * @param zipArchive            the classes folder
     * @param sourceFolders         the resources
     * @param jarFiles              the embedded java files
     * @param nativeFolders         the native folders
     * @param signWithDebugKeyStore enables the signature of the APK using the debug key
     * @throws MojoExecutionException if the APK cannot be created.
     */
    private void doAPKWithAPKBuilder(File outputFile, File dexFile, File zipArchive, Collection<File> sourceFolders,
                                     List<File> jarFiles, Collection<File> nativeFolders, boolean signWithDebugKeyStore)
            throws MojoExecutionException {
        getLog().debug("Building APK with internal APKBuilder");

        //A when jack is running the classes directory will not get filled (usually)
        // so let's skip it if it wasn't created by something else
        if (projectOutputDirectory.exists() || !getJack().isEnabled()) { // NOSONAR : Yes IntelliJ
            sourceFolders.add(projectOutputDirectory);
        }

        collectApkArtifacts(jarFiles);

        for (File src : sourceFolders) {
            computeDuplicateFilesInSource(src);
        }

        // Check duplicates.
        if (extractDuplicates) {
            try {
                extractDuplicateFiles(jarFiles, sourceFolders);
            } catch (IOException e) {
                getLog().error("Could not extract duplicates to duplicate-resources.jar", e);
            }
        }

        try {
            final String debugKeyStore = signWithDebugKeyStore ? ApkBuilder.getDebugKeystore() : null;
            final ApkBuilder apkBuilder = new ApkBuilder(outputFile, zipArchive, dexFile, debugKeyStore, null);
            if (Boolean.TRUE.equals(apkDebug)) {
                apkBuilder.setDebugMode(true);
            }

            for (File sourceFolder : sourceFolders) {
                getLog().debug("Adding source folder : " + sourceFolder);
                // Use ApkBuilder#addFile() to explicitly add resource files so that we can add META-INF/services.
                addResourcesFromFolder(apkBuilder, sourceFolder);
            }

            addJarResourcesToAPK(jarFiles, apkBuilder);
            addSecondaryDexes(dexFile, apkBuilder);

            for (File nativeFolder : nativeFolders) {
                getLog().debug("Adding native library : " + nativeFolder);
                apkBuilder.addNativeLibraries(nativeFolder);
            }
            apkBuilder.sealApk();
        } catch (ApkCreationException | SealedApkException | IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (DuplicateFileException e) {
            final String msg = String.format("Duplicated file: %s, found in archive %s and %s",
                    e.getArchivePath(), e.getFile1(), e.getFile2());
            throw new MojoExecutionException(msg, e);
        }
    }

    /**
     * Adds jar resources to the APK builder.
     *
     * @param jarFiles   the jar files
     * @param apkBuilder the APK builder
     * @throws ApkCreationException   if an error occurs while creating the APK
     * @throws SealedApkException     if the APK is sealed
     * @throws DuplicateFileException if a duplicate file is found
     */
    private void addJarResourcesToAPK(@Nonnull List<File> jarFiles, ApkBuilder apkBuilder)
            throws ApkCreationException, SealedApkException, DuplicateFileException {
        for (File jarFile : jarFiles) {
            boolean excluded = false;

            if (excludeJarResourcesPatterns != null) {
                excluded = processExclusionPatterns(jarFile);
            }

            if (excluded) {
                continue;
            }

            if (jarFile.isDirectory()) {
                getLog().debug("Adding resources from jar folder : " + jarFile);
                final String[] filenames = jarFile.list((dir, name) ->
                        PATTERN_JAR_EXT.matcher(name).matches());

                for (String filename : Objects.requireNonNull(filenames)) {
                    final File innerJar = new File(jarFile, filename);

                    getLog().debug("Adding resources from innerJar : " + innerJar);
                    apkBuilder.addResourcesFromJar(innerJar);
                }
            } else {
                getLog().debug("Adding resources from : " + jarFile);
                apkBuilder.addResourcesFromJar(jarFile);
            }
        }
    }

    private boolean processExclusionPatterns(@Nonnull File jarFile) {
        boolean excluded = false;
        final String name = jarFile.getName();

        getLog().debug("Checking " + name + " against patterns");
        for (Pattern pattern : excludeJarResourcesPatterns) {
            final Matcher matcher = pattern.matcher(name);
            if (matcher.matches()) {
                getLog().debug("Jar " + name + " excluded by pattern " + pattern);
                excluded = true;
                break;
            } else {
                getLog().debug("Jar " + name + " not excluded by pattern " + pattern);
            }
        }
        return excluded;
    }

    /**
     * Collects all APK artifacts.
     *
     * @param jarFiles the jar files
     */
    private void collectApkArtifacts(List<File> jarFiles) {
        for (Artifact artifact : filterArtifacts(getRelevantCompileArtifacts(), skipDependencies,
                artifactTypeSet.getIncludes(), artifactTypeSet.getExcludes(), artifactSet.getIncludes(),
                artifactSet.getExcludes())) {
            getLog().debug("Found artifact for APK :" + artifact);
            if (extractDuplicates) {
                try {
                    computeDuplicateFiles(artifact.getFile());
                } catch (Exception e) {
                    getLog().warn("Cannot compute duplicates files from " +
                            artifact.getFile().getAbsolutePath(), e);
                }
            }
            jarFiles.add(artifact.getFile());
        }
    }

    /**
     * Collect all Files from Folder (recursively) that are not class files.
     *
     * @param folder         the folder
     * @param collectedFiles the collected files
     */
    private void collectFiles(@Nonnull File folder, final List<File> collectedFiles) {
        //noinspection ResultOfMethodCallIgnored
        folder.listFiles(file -> {
            if (file.isDirectory()) {
                collectFiles(file, collectedFiles);
            } else if (file.isFile() &&
                    !file.getName().endsWith(".class")) {
                collectedFiles.add(file);
            }

            return false;
        });
    }

    /**
     * Adds all non-class files from folder, so that we can add META-INF/services resources.
     *
     * @param builder the APK builder
     * @param folder  the folder
     * @throws ApkCreationException   if the APK cannot be created.
     * @throws SealedApkException     if the APK is sealed
     * @throws DuplicateFileException if a duplicate file is found
     * @throws IOException            if an I/O error occurs
     */
    private void addResourcesFromFolder(ApkBuilder builder, @Nonnull File folder)
            throws SealedApkException, DuplicateFileException, ApkCreationException, IOException {
        final int folderPathLength = folder.getCanonicalPath().length();

        final List<File> resourceFiles = new ArrayList<>();
        collectFiles(folder, resourceFiles);

        for (final File resourceFile : resourceFiles) {
            final String resourceName = resourceFile
                    .getCanonicalPath()
                    .substring(folderPathLength + 1)
                    .replace("\\\\", "/");
            getLog().info("Adding resource " + resourceFile + " : " + resourceName);
            builder.addFile(resourceFile, resourceName);
        }
    }

    /**
     * @param dexFile    the dex file
     * @param apkBuilder the APK builder
     * @throws ApkCreationException   if the APK cannot be created.
     * @throws SealedApkException     if the APK is sealed
     * @throws DuplicateFileException if a duplicate file is found
     */
    private void addSecondaryDexes(File dexFile, ApkBuilder apkBuilder) throws ApkCreationException,
            SealedApkException, DuplicateFileException {
        int dexNumber = 2;
        String dexFileName = getNextDexFileName(dexNumber);
        File secondDexFile = createNextDexFile(dexFile, dexFileName);
        while (secondDexFile.exists()) {
            apkBuilder.addFile(secondDexFile, dexFileName);
            dexNumber++;
            dexFileName = getNextDexFileName(dexNumber);
            secondDexFile = createNextDexFile(dexFile, dexFileName);
        }
    }

    /**
     * @param dexFile     the dex file
     * @param dexFileName the name of the dex file
     * @return the next dex file
     */
    @Nonnull
    private File createNextDexFile(@Nonnull File dexFile, String dexFileName) {
        return new File(dexFile.getParentFile(), dexFileName);
    }

    /**
     * @param dexNumber the number of the dex file
     * @return the name of the dex file
     */
    @Nonnull
    private String getNextDexFileName(int dexNumber) {
        return CLASSES + dexNumber + DEX_SUFFIX;
    }

    /**
     * @param in              the input jar file
     * @param duplicates      the list of duplicates found in the jar
     * @param duplicatesAdded the set of duplicates already added to the duplicate-resources.jar
     * @param duplicateZos    the duplicate-resources.jar
     * @param num             the number of the duplicate-resources.jar
     * @return the new jar file or null if the output file already exists
     */
    @Nullable
    private File removeDuplicatesFromJar(@Nonnull File in, List<String> duplicates,
                                         Set<String> duplicatesAdded, ZipOutputStream duplicateZos, int num) {
        String target = targetDirectory.getAbsolutePath();
        File tmp = new File(target, "unpacked-embedded-jars");
        boolean d = tmp.mkdirs();
        if (d) getLog().debug("Directory unpacked-embedded-jars created");
        String jarName = String.format("%s-%d.%s",
                Files.getNameWithoutExtension(in.getName()), num,
                Files.getFileExtension(in.getName()));
        File out = new File(tmp, jarName);

        if (out.exists()) {
            return out;
        } else {
            try {
                boolean b = out.createNewFile();
                if (b) {
                    getLog().debug(String.format("Remove Duplicates: %s created", out.getAbsolutePath()));
                }
            } catch (IOException e) {
                getLog().warn("removeDuplicatesFromJar: " + Arrays.toString(e.getStackTrace()));
            }
        }

        // Create a new Jar file
        return createNewJarFile(in, duplicates, duplicatesAdded, duplicateZos, out);
    }

    @Nullable
    private File createNewJarFile(@NotNull File in, List<String> duplicates, Set<String> duplicatesAdded,
                                  ZipOutputStream duplicateZos, File out) {
        try (FileOutputStream fos = new FileOutputStream(out);
             ZipOutputStream jos = new ZipOutputStream(new BufferedOutputStream(fos));
             ZipFile inZip = new ZipFile(in)) {

            // Use List instead of Enumeration for better performance and modern syntax
            List<? extends ZipEntry> entries = Collections.list(inZip.entries());

            for (ZipEntry entry : entries) {
                String entryName = entry.getName();

                if (duplicates.contains(entryName)) {
                    // Process duplicate entries
                    processDuplicateEntry(inZip, entry,
                            (transformers != null ? List.of(transformers) : null),
                            duplicatesAdded, duplicateZos);
                } else {
                    // Process non-duplicate entries
                    processNonDuplicateEntry(jos, inZip, entry);
                }
            }

            getLog().info(String.format("%s rewritten without duplicates: %s", in.getName(), out.getAbsolutePath()));
            return out;

        } catch (FileNotFoundException e) {
            getLog().error(String.format("Cannot remove duplicates: the output file %s not found", out.getAbsolutePath()));
            return null;
        } catch (IOException e) {
            getLog().error("Cannot remove duplicates: " + e.getMessage());
            return null;
        }
    }

    /**
     * Process a non-duplicate entry in the jar file.
     *
     * @param jos   The ZipOutputStream to write the entry to.
     * @param inZip The input jar file.
     * @param entry The entry to process.
     * @throws IOException If an I/O error occurs.
     */
    private void processNonDuplicateEntry(@Nonnull ZipOutputStream jos, @Nonnull ZipFile inZip, ZipEntry entry)
            throws IOException {
        jos.putNextEntry(entry);
        try (InputStream currIn = inZip.getInputStream(entry)) {
            copyStreamWithoutClosing(currIn, jos);
        }
        jos.closeEntry();
    }

    /**
     * Process a duplicate entry in the jar file.
     *
     * @param inZip           The input jar file.
     * @param entry           The duplicate entry.
     * @param transformers    The list of transformers to apply to the resource.
     * @param duplicatesAdded The set of duplicates already added to the duplicates jar.
     * @param duplicateZos    The ZipOutputStream to write the duplicates to.
     * @throws IOException If an I/O error occurs.
     */
    private void processDuplicateEntry(ZipFile inZip, @Nonnull ZipEntry entry,
                                       List<ReproducibleResourceTransformer> transformers, Set<String> duplicatesAdded,
                                       ZipOutputStream duplicateZos) throws IOException {

        String entryName = entry.getName();

        // Try to transform the resource
        if (transformers != null && tryTransformResource(inZip, entry, transformers)) {
            return;
        }

        // Handle untransformed duplicates
        if (!duplicatesAdded.contains(entryName)) {
            duplicatesAdded.add(entryName);
            duplicateZos.putNextEntry(entry);
            try (InputStream currIn = inZip.getInputStream(entry)) {
                copyStreamWithoutClosing(currIn, duplicateZos);
            }
            duplicateZos.closeEntry();
        }
    }

    private boolean tryTransformResource(ZipFile inZip, ZipEntry entry,
                                         @Nonnull List<ReproducibleResourceTransformer> transformers)
            throws IOException {

        for (ReproducibleResourceTransformer transformer : transformers) {
            if (transformer.canTransformResource(entry.getName())) {
                getLog().info(String.format("Transforming %s using %s",
                        entry.getName(), transformer.getClass().getName()));

                try (InputStream currIn = inZip.getInputStream(entry)) {
                    transformer.processResource(entry.getName(), currIn, null, 0);
                }
                return true;
            }
        }
        return false;
    }


    /**
     * @param root            The root folder to remove duplicates from.
     * @param in              The folder to remove duplicates from.
     * @param duplicates      The list of duplicates to remove.
     * @param duplicatesAdded The set of duplicates already added to the duplicates jar.
     * @param duplicateZos    The ZipOutputStream to write the duplicates to.
     */
    private void removeDuplicatesFromFolder(@Nonnull File root, @Nonnull File in, List<String> duplicates,
                                            Set<String> duplicatesAdded, ZipOutputStream duplicateZos) {
        String rPath = root.getAbsolutePath();
        try {
            for (File f : Objects.requireNonNull(in.listFiles())) {
                if (f.isDirectory()) {
                    removeDuplicatesFromFolder(root, f, duplicates, duplicatesAdded, duplicateZos);
                } else {
                    handleDuplicateFile(duplicates, duplicatesAdded, duplicateZos, f, rPath);
                }
            }
        } catch (IOException e) {
            getLog().error("Cannot removing duplicates : " + e.getMessage());
        }
    }

    private void handleDuplicateFile(@Nonnull List<String> duplicates, Set<String> duplicatesAdded,
                                     ZipOutputStream duplicateZos, @Nonnull File f, @Nonnull String rPath)
            throws IOException {
        String lName = f.getAbsolutePath();
        lName = lName.substring(rPath.length() + 1); //make relative path
        if (duplicates.contains(lName)) {
            boolean resourceTransformed = false;
            if (transformers != null) {
                for (ReproducibleResourceTransformer transformer : transformers) {
                    if (transformer.canTransformResource(lName)) {
                        getLog().info("Transforming " + lName
                                + " using " + transformer.getClass().getName());
                        try (FileInputStream currIn = new FileInputStream(f)) {
                            transformer.processResource(lName, currIn, null, 0);
                            resourceTransformed = true;
                        }
                        break;
                    }
                }
            }
            //if not handled by transformer, add (once) to duplicates jar
            if (!resourceTransformed &&
                    !duplicatesAdded.contains(lName)) {
                duplicatesAdded.add(lName);
                ZipEntry entry = new ZipEntry(lName);
                duplicateZos.putNextEntry(entry);
                try (FileInputStream currIn = new FileInputStream(f)) {
                    copyStreamWithoutClosing(currIn, duplicateZos);
                }
                duplicateZos.closeEntry();
            }
            java.nio.file.Files.delete(f.toPath());
        }
    }

    /**
     * @return The collection of native folders to be included in the APK.
     * @throws MojoExecutionException if there is a problem resolving the artifacts.
     */
    @Nonnull
    private Collection<File> getNativeLibraryFolders() throws MojoExecutionException {
        final List<File> natives = new ArrayList<>();
        final boolean isDebugEnabled = getLog().isDebugEnabled();

        if (nativeLibrariesDirectory.exists()) {
            // If we have prebuilt native libs then copy them over to the native output folder.
            // NB they will be copied over the top of any native libs generated as part of the NdkBuildMojo
            copyLocalNativeLibraries(nativeLibrariesDirectory, ndkOutputDirectory);
        }

        final Set<Artifact> artifacts = getNativeLibraryArtifacts();
        processNativeLibraries(artifacts, isDebugEnabled, natives);

        return natives;
    }

    /**
     * Process the native libraries for the Android APK build. This includes collecting native libraries
     * from AAR artifacts, copying architecture-specific native dependencies, and setting up
     * debug-related native components.
     *
     * @param artifacts      The set of artifacts to process for native libraries. Must not be null.
     * @param isDebugEnabled Whether debug logging is enabled. If true, additional debug information
     *                       will be logged during processing.
     * @param natives        The list to which native library folders will be added. This list is modified
     *                       during execution to include all discovered native library locations.
     * @throws MojoExecutionException if there is a problem processing the native libraries, such as
     *                                IO errors during file copying or invalid artifact configurations.
     */
    private void processNativeLibraries(@Nonnull Set<Artifact> artifacts, boolean isDebugEnabled, List<File> natives)
            throws MojoExecutionException {
        for (Artifact resolvedArtifact : artifacts) {
            if (AAR.equals(resolvedArtifact.getType())) {
                // If the artifact is an AAR or APKLIB then add their native libs folder to the result.
                final File folder = getUnpackedLibNativesFolder(resolvedArtifact);
                getLog().debug("Adding native library folder " + folder);
                natives.add(folder);
            }

            // Copy the native lib dependencies into the native lib output folder
            for (String ndkArchitecture : AndroidNdk.NDK_ARCHITECTURES) {
                if (NativeHelper.artifactHasHardwareArchitecture(resolvedArtifact,
                        ndkArchitecture, nativeLibrariesDependenciesHardwareArchitectureDefault)) {
                    // If the artifact is a native lib then copy it into the native libs output folder.
                    copyNativeLibraryArtifact(resolvedArtifact, ndkOutputDirectory, ndkArchitecture);
                }
            }
        }

        if (Boolean.TRUE.equals(apkDebug)) {
            // Copy the gdbserver binary into the native libs output folder (for each architecture).
            for (String ndkArchitecture : AndroidNdk.NDK_ARCHITECTURES) {
                copyGdbServer(ndkOutputDirectory, ndkArchitecture);
            }
        }

        // If we have any native libs in the native output folder then add the output folder to the result.
        if (ndkOutputDirectory.exists()) {
            if (isDebugEnabled)
                getLog().debug("Adding built native library folder " + ndkOutputDirectory);
            natives.add(ndkOutputDirectory);
        }
    }

    /**
     * @return Any native dependencies or attached artifacts. This may include artifacts from the ndk-build MOJO.
     * @throws MojoExecutionException if there is a problem resolving the artifacts.
     */
    private Set<Artifact> getNativeLibraryArtifacts() throws MojoExecutionException {
        return getNativeHelper().getNativeDependenciesArtifacts(this, getUnpackedLibsDirectory(), true);
    }

    /**
     * @param artifact             The artifact to copy the native library from.
     * @param destinationDirectory The directory to copy the native library to.
     * @param ndkArchitecture      The architecture to copy the native library for.
     * @throws MojoExecutionException if an error occurs during the copy process
     */
    private void copyNativeLibraryArtifact(Artifact artifact,
                                           File destinationDirectory,
                                           String ndkArchitecture) throws MojoExecutionException {

        final File artifactFile = getArtifactResolverHelper().resolveArtifactToFile(artifact);
        try {
            final String artifactId = artifact.getArtifactId();
            String filename = artifactId.startsWith("lib")
                    ? artifactId + ".so"
                    : "lib" + artifactId + ".so";
            if (ndkFinalLibraryName != null
                    && artifact.getFile().getName().startsWith("lib" + ndkFinalLibraryName)) {
                // The artifact looks like one we built with the NDK in this module
                // preserve the name from the NDK build
                filename = artifact.getFile().getName();
            }

            final File folder = new File(destinationDirectory, ndkArchitecture);
            final File file = new File(folder, filename);
            if (getLog().isDebugEnabled())
                getLog().debug("Copying native dependency " + artifactId +
                        " (" + artifact.getGroupId() + ") to " + file);
            FileUtils.copyFile(artifactFile, file);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not copy native dependency.", e);
        }
    }

    /**
     * Copy the Ndk GdbServer into the architecture output folder if the folder exists but the GdbServer doesn't.
     *
     * @param destinationDirectory The directory to copy the gdbserver binary to.
     * @param architecture         The architecture to copy the gdbserver binary for.
     * @throws MojoExecutionException if an error occurs during the copy process
     */
    private void copyGdbServer(File destinationDirectory, String architecture)
            throws MojoExecutionException {

        try {
            final File destDir = new File(destinationDirectory, architecture);
            if (destDir.exists()) {
                // Copy the gdbserver binary to libs/<architecture>/
                final File gdbServerFile = getAndroidNdk().getGdbServer(architecture);
                final File destFile = new File(destDir, "gdbserver");
                if (!destFile.exists()) {
                    getLog().debug("Copying gdbServer to " + destFile);
                    FileUtils.copyFile(gdbServerFile, destFile);
                } else {
                    getLog().info("Note: gdbserver binary already exists at destination, will not copy over");
                }
            }
        } catch (Exception e) {
            getLog().error("Error while copying gdbserver: " + e.getMessage(), e);
            throw new MojoExecutionException("Error while copying gdbserver: " + e.getMessage(), e);
        }

    }

    /**
     * @param localNativeLibrariesDirectory The directory containing the native libraries to copy from.
     * @param destinationDirectory          The directory to copy the native libraries to.
     * @throws MojoExecutionException if an error occurs during the copy process
     */
    private void copyLocalNativeLibraries(final File localNativeLibrariesDirectory,
                                          final File destinationDirectory)
            throws MojoExecutionException {

        getLog().debug("Copying existing native libraries from " + localNativeLibrariesDirectory);
        try {

            IOFileFilter libSuffixFilter = FileFilterUtils.suffixFileFilter(".so");

            IOFileFilter gdbserverNameFilter = FileFilterUtils.nameFileFilter("gdbserver");
            IOFileFilter orFilter = FileFilterUtils.or(libSuffixFilter, gdbserverNameFilter);

            IOFileFilter libFiles = FileFilterUtils.and(FileFilterUtils.fileFileFilter(), orFilter);
            FileFilter filter = FileFilterUtils.or(DirectoryFileFilter.DIRECTORY, libFiles);
            FileUtils
                    .copyDirectory(localNativeLibrariesDirectory, destinationDirectory, filter);

        } catch (IOException e) {
            getLog().error("Could not copy native libraries: " + e.getMessage(), e);
            throw new MojoExecutionException("Could not copy native dependency.", e);
        }
    }

    /**
     * Generates an intermediate apk file (actually .ap_) containing the resources and assets.
     *
     * @throws MojoExecutionException if an error occurs during the apk generation process
     */
    private void generateIntermediateApk() throws MojoExecutionException {
        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommandExecutor();
        executor.setLogger(this.getLog());
        File[] overlayDirectories = getResourceOverlayDirectories();

        File androidJar = getAndroidSdk().getAndroidJar();
        File outputFile = new File(targetDirectory, finalName + ".ap_");

        List<File> dependencyArtifactResDirectoryList = new ArrayList<>();
        for (Artifact libraryArtifact : getTransitiveDependencyArtifacts(AAR)) {
            final File libraryResDir = getUnpackedLibResourceFolder(libraryArtifact);
            if (libraryResDir.exists()) {
                dependencyArtifactResDirectoryList.add(libraryResDir);
            }
        }

        AaptCommandBuilder commandBuilder = AaptCommandBuilder
                .packageResources(getLog())
                .forceOverwriteExistingFiles()
                .setPathToAndroidManifest(destinationManifestFile)
                .addResourceDirectoriesIfExists(overlayDirectories)
                .addResourceDirectoryIfExists(resourceDirectory)
                .addResourceDirectoriesIfExists(dependencyArtifactResDirectoryList)
                .autoAddOverlay()
                // NB aapt only accepts a single assets parameter - combinedAssets is a merge of all assets
                .addRawAssetsDirectoryIfExists(combinedAssets)
                .renameManifestPackage(renameManifestPackage)
                .renameInstrumentationTargetPackage(renameInstrumentationTargetPackage)
                .addExistingPackageToBaseIncludeSet(androidJar)
                .setOutputApkFile(outputFile)
                .addConfigurations(configurations)
                .setVerbose(aaptVerbose)
                .setDebugMode(!release)
                .addExtraArguments(aaptExtraArgs);

        if (getLog().isDebugEnabled())
            getLog().debug(getAndroidSdk().getAaptPath() + " " + commandBuilder.toString());
        try {
            executor.setCaptureStdOut(true);
            List<String> commands = commandBuilder.build();
            executor.executeCommand(getAndroidSdk().getAaptPath(), commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }
    }

    /**
     * @return an AndroidSigner instance based on the signDebug parameter.
     */
    protected AndroidSigner getAndroidSigner() {
        if (sign == null) {
            return new AndroidSigner(signDebug);
        } else {
            return new AndroidSigner(sign.getDebug());
        }
    }

}
