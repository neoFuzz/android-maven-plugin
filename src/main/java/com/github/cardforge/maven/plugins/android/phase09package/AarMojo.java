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

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.github.cardforge.maven.plugins.android.AbstractAndroidMojo;
import com.github.cardforge.maven.plugins.android.CommandExecutor;
import com.github.cardforge.maven.plugins.android.ExecutionException;
import com.github.cardforge.maven.plugins.android.IncludeExcludeSet;
import com.github.cardforge.maven.plugins.android.common.AaptCommandBuilder;
import com.github.cardforge.maven.plugins.android.common.AndroidExtension;
import com.github.cardforge.maven.plugins.android.common.NativeHelper;
import com.github.cardforge.maven.plugins.android.config.PullParameter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static com.github.cardforge.maven.plugins.android.InclusionExclusionResolver.filterArtifacts;
import static com.github.cardforge.maven.plugins.android.common.AndroidExtension.AAR;
import static com.github.cardforge.maven.plugins.android.common.AndroidExtension.APKLIB;


/**
 * Creates an Android Archive (aar) file.<br>
 */
@Mojo(
        name = "aar",
        defaultPhase = LifecyclePhase.VERIFY,
        requiresDependencyResolution = ResolutionScope.COMPILE
)
public class AarMojo extends AbstractAndroidMojo {
    /**
     * The name of the top level folder in the AAR where native libraries are found.
     * NOTE: This is inconsistent with APK where the folder is called "lib", and does not match APKLIB
     * layout either, where the folder is called "libs".
     */
    public static final String NATIVE_LIBRARIES_FOLDER = "jni";

    /**
     * The name of the top level folder in the AAR where JAR libraries are found.
     */
    public static final String LIBRARIES_FOLDER = "libs";
    public static final String FILE = " file.";
    public static final String IOEXCEPTION_WHILE_CREATING = "IOException while creating .";

    /**
     * <p>Classifier to add to the artifact generated. If given, the artifact will be an attachment instead.</p>
     */
    @Parameter
    private String classifier;

    /**
     * Specifies the application makefile to use for the build (if other than the default {@code Application.mk}).
     */
    @Parameter
    @PullParameter
    private String applicationMakefile;

    /**
     * Defines the architecture for the NDK build
     */
    @Parameter(property = "android.ndk.build.architecture")
    @PullParameter
    private String ndkArchitecture;

    /**
     * Specifies the classifier with which the artifact should be stored in the repository
     */
    @Parameter(property = "android.ndk.build.native-classifier")
    @PullParameter
    private String ndkClassifier;

    /**
     * Specifies the files that should be included in the classes.jar within the aar
     */
    @Parameter
    @PullParameter
    private String[] classesJarIncludes = new String[]{"**/*"};

    /**
     * Specifies the files that should be excluded from the classes.jar within the aar
     */
    @Parameter
    @PullParameter
    private String[] classesJarExcludes = new String[]{"**/R.class", "**/R$*.class"};

    /**
     * Specifies the proguard rule files to be included in the final package. All specified files will be merged into
     * one proguard.txt file.
     */
    @Parameter
    private File[] consumerProguardFiles;

    @Parameter(
            property = "android.proguard.obfuscatedJar",
            defaultValue = "${project.build.directory}/${project.build.finalName}_obfuscated.jar"
    )
    private String obfuscatedJar;

    /**
     * Set to {@code false} to automatically include all dependency JARs in the generated AAR. These are placed in
     * a directory called {@code libs} in the generated aar. The set of JARs to include can be restricted using the
     * {@code artifactSet} and {@code artifactTypeSet} parameters. If {@code skipDependencies} is {@code true}
     * (default), only those JARs explicitly selected by the {@code artifactSet} and {@code artifactTypeSet} parameters
     * will be included.
     */
    @Parameter(property = "skipDependencies", defaultValue = "true")
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
     *             &lt;include&gt;bar-group:bar-artifact&lt;/include&gt;
     *             &lt;include&gt;baz-group&lt;/include&gt;
     *         &lt;includes&gt;
     *         &lt;excludes&gt;
     *             &lt;exclude&gt;qux-group:qux-artifact&lt;/exclude&gt;
     *         &lt;excludes&gt;
     *     &lt;/artifactTypeSet&gt;
     * </pre>
     */
    @Parameter(property = "artifactSet")
    private IncludeExcludeSet artifactSet;

    private List<String> sourceFolders = new ArrayList<>();

    /**
     * @throws MojoExecutionException if an error occurs during the execution of the Mojo
     * @throws MojoFailureException if an error occurs during the execution of the Mojo
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        String out = targetDirectory.getPath();
        for (String src : project.getCompileSourceRoots()) {
            if (!src.startsWith(out)) {
                sourceFolders.add(src);
            }
        }

        getLog().info("Generating AAR file : " + project.getArtifactId());
        generateIntermediateApk();

        final File outputFile = createAarLibraryFile(createAarClassesJar());

        if (classifier == null) {
            // Set the generated file as the main artifact (because the pom states <packaging>aar</packaging>)
            project.getArtifact().setFile(outputFile);
        } else {
            // If there is a classifier specified, attach the artifact using that
            projectHelper.attachArtifact(project, AndroidExtension.AAR, classifier, outputFile);
        }
    }

    /**
     * Creates an appropriate aar/classes.jar that does not include R
     *
     * @return File which is the AAR classes jar.
     * @throws MojoExecutionException if an error occurs while creating the classes.jar file
     */
    protected File createAarClassesJar() throws MojoExecutionException {
        final File obfuscatedJarFile = new File(obfuscatedJar);
        if (obfuscatedJarFile.exists()) {
            attachJar(obfuscatedJarFile);
            return obfuscatedJarFile;
        }

        final File classesJar = new File(targetDirectory, finalName + ".aar.classes.jar");
        try {
            JarArchiver jarArchiver = new JarArchiver();
            jarArchiver.setDestFile(classesJar);
            jarArchiver.addDirectory(projectOutputDirectory,
                    classesJarIncludes,
                    classesJarExcludes);
            jarArchiver.createArchive();
            attachJar(classesJar);
            return classesJar;
        } catch (ArchiverException e) {
            throw new MojoExecutionException("ArchiverException while creating ." + classesJar + FILE, e);
        } catch (IOException e) {
            throw new MojoExecutionException(IOEXCEPTION_WHILE_CREATING + classesJar + FILE, e);
        }

    }

    /**
     * @param jarFile The JAR file to attach to the project.
     */
    private void attachJar(File jarFile) {
        if (attachJar) {
            projectHelper.attachArtifact(project, "jar", project.getArtifact().getClassifier(), jarFile);
        }
    }

    /**
     * @return AAR file.
     * @throws MojoExecutionException if an error occurs while creating the AAR file
     */
    protected File createAarLibraryFile(File classesJar) throws MojoExecutionException {
        final File aarLibrary = new File(targetDirectory,
                finalName + "." + AAR);
        FileUtils.deleteQuietly(aarLibrary);

        try {
            final ZipArchiver zipArchiver = new ZipArchiver();
            zipArchiver.setDestFile(aarLibrary);

            zipArchiver.addFile(destinationManifestFile, "AndroidManifest.xml");
            addDirectory(zipArchiver, assetsDirectory, "assets", false);

            // res folder must be included in the archive even if empty or non-existent.
            if (!resourceDirectory.exists()) {
                resourceDirectory.mkdir();
            }
            addDirectory(zipArchiver, resourceDirectory, "res", true);

            zipArchiver.addFile(classesJar, SdkConstants.FN_CLASSES_JAR);

            final File[] overlayDirectories = getResourceOverlayDirectories();
            for (final File resOverlayDir : overlayDirectories) {
                if (resOverlayDir != null && resOverlayDir.exists()) {
                    addDirectory(zipArchiver, resOverlayDir, "res", false);
                }
            }

            if (consumerProguardFiles != null) {
                final File mergedConsumerProguardFile = new File(targetDirectory, "consumer-proguard.txt");
                if (mergedConsumerProguardFile.exists()) {
                    FileUtils.forceDelete(mergedConsumerProguardFile);
                }
                mergedConsumerProguardFile.createNewFile();
                StringBuilder mergedConsumerProguardFileBuilder = new StringBuilder();
                for (File consumerProguardFile : consumerProguardFiles) {
                    if (consumerProguardFile.exists()) {
                        getLog().info("Adding consumer proguard file " + consumerProguardFile);
                        FileInputStream consumerProguardFileInputStream = null;
                        try {
                            consumerProguardFileInputStream = new FileInputStream(consumerProguardFile);
                            mergedConsumerProguardFileBuilder.append(
                                    IOUtils.toString(consumerProguardFileInputStream));
                            mergedConsumerProguardFileBuilder.append(SystemUtils.LINE_SEPARATOR);
                        } catch (IOException e) {
                            throw new MojoExecutionException("Error writing consumer proguard file ", e);
                        } finally {
                            IOUtils.closeQuietly(consumerProguardFileInputStream);
                        }
                    }
                }
                FileOutputStream mergedConsumerProguardFileOutputStream = null;
                try {
                    mergedConsumerProguardFileOutputStream = new FileOutputStream(mergedConsumerProguardFile);
                    IOUtils.write(mergedConsumerProguardFileBuilder, mergedConsumerProguardFileOutputStream);
                } catch (IOException e) {
                    throw new MojoExecutionException("Error writing consumer proguard file ", e);
                } finally {
                    IOUtils.closeQuietly(mergedConsumerProguardFileOutputStream);
                }

                zipArchiver.addFile(mergedConsumerProguardFile, "proguard.txt");
            }

            addR(zipArchiver);

            // Lastly, add any native libraries
            addNativeLibraries(zipArchiver);

            // ... and JAR libraries
            addLibraries(zipArchiver);

            zipArchiver.createArchive();
        } catch (ArchiverException e) {
            throw new MojoExecutionException("ArchiverException while creating ." + AAR + FILE, e);
        } catch (IOException e) {
            throw new MojoExecutionException(IOEXCEPTION_WHILE_CREATING + AAR + FILE, e);
        }

        return aarLibrary;
    }

    /**
     * @param zipArchiver ZipArchiver to add files to.
     * @throws MojoExecutionException if an error occurs while adding the R.txt file to the archive
     * @throws IOException if an error occurs while creating the R.txt file
     */
    private void addR(ZipArchiver zipArchiver) throws MojoExecutionException, IOException {
        final File rFile = new File(targetDirectory, "R.txt");
        if (!rFile.exists()) {
            getLog().debug("No resources - creating empty R.txt");
            if (!rFile.createNewFile()) {
                getLog().warn("Unable to create R.txt in AAR");
            }
        }
        zipArchiver.addFile(rFile, "R.txt");
        getLog().debug("Packaging R.txt in AAR");
    }

    /**
     * @param zipArchiver ZipArchiver to add files to.
     * @throws MojoExecutionException if an error occurs while adding native libraries to the archive
     */
    private void addNativeLibraries(final ZipArchiver zipArchiver) throws MojoExecutionException {
        try {
            if (nativeLibrariesDirectory.exists()) {
                getLog().info(nativeLibrariesDirectory + " exists, adding libraries.");
                addDirectory(zipArchiver, nativeLibrariesDirectory, NATIVE_LIBRARIES_FOLDER, false);
            } else {
                getLog().info(nativeLibrariesDirectory
                        + " does not exist, looking for libraries in target directory.");
                // Add native libraries built and attached in this build
                String[] ndkArchitectures = NativeHelper.getNdkArchitectures(ndkArchitecture,
                        applicationMakefile,
                        project.getBasedir());
                for (String architecture : ndkArchitectures) {
                    final File ndkLibsDirectory = new File(ndkOutputDirectory, architecture);
                    addSharedLibraries(zipArchiver, ndkLibsDirectory, architecture);

                    // Add native library dependencies
                    // FIXME: Remove as causes duplicate libraries when building final APK if this set includes
                    //        libraries from dependencies of the AAR
                    //final File dependentLibs = new File( ndkOutputDirectory.getAbsolutePath(), ndkArchitecture );
                    //addSharedLibraries( jarArchiver, dependentLibs, prefix );
                }
            }
        } catch (ArchiverException e) {
            throw new MojoExecutionException(IOEXCEPTION_WHILE_CREATING + AAR + FILE, e);
        }
        // TODO: Next is to check for any:
        // TODO: - compiled in (as part of this build) libs
        // TODO:    - That is of course easy if the artifact is indeed attached
        // TODO:    - If not attached, it gets a little trickier  - check the target dir for any compiled .so files (generated by NDK mojo)
        // TODO:        - But where is that directory configured?
    }

    /**
     * @param zipArchiver ZipArchiver to add files to.
     * @throws MojoExecutionException if an error occurs while adding JAR libraries to the archive
     */
    private void addLibraries(final ZipArchiver zipArchiver) throws MojoExecutionException {
        for (Artifact artifact : filterArtifacts(getRelevantCompileArtifacts(), skipDependencies,
                artifactTypeSet.getIncludes(), artifactTypeSet.getExcludes(), artifactSet.getIncludes(),
                artifactSet.getExcludes())) {
            getLog().debug("Include library in AAR :" + artifact);

            zipArchiver.addFile(artifact.getFile(), LIBRARIES_FOLDER + "/" + artifact.getFile().getName());
        }
    }

    /**
     * Makes sure the string ends with "/"
     *
     * @param prefix any string, or null.
     * @return the prefix with a "/" at the end, never null.
     */
    protected String endWithSlash(String prefix) {
        prefix = StringUtils.defaultIfEmpty(prefix, "/");
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        return prefix;
    }

    /**
     * Adds a directory to a {@link JarArchiver} with a directory prefix.
     *
     * @param zipArchiver         ZipArchiver to use to archive the file.
     * @param directory           The directory to add.
     * @param prefix              An optional prefix for where in the Jar file the directory's contents should go.
     * @param includeEmptyFolders Whether to include an entry for empty folder in the archive.
     */
    protected void addDirectory(ZipArchiver zipArchiver, File directory, String prefix, boolean includeEmptyFolders) {
        if (directory != null && directory.exists()) {
            final DefaultFileSet fileSet = new DefaultFileSet();
            fileSet.setPrefix(endWithSlash(prefix));
            fileSet.setDirectory(directory);
            fileSet.setIncludingEmptyDirectories(includeEmptyFolders);
            zipArchiver.addFileSet(fileSet);
            getLog().debug("Added files from " + directory);
        }
    }

    /**
     * Adds all shared libraries (.so) to a {@link JarArchiver} under 'jni'.
     *
     * @param zipArchiver  The jarArchiver to add files to
     * @param directory    The directory to scan for .so files
     * @param architecture The prefix for where in the jar the .so files will go.
     */
    protected void addSharedLibraries(ZipArchiver zipArchiver, @NonNull File directory, String architecture) {
        getLog().debug("Searching for shared libraries in " + directory);
        File[] libFiles = directory.listFiles((dir, name) -> name.startsWith("lib") && name.endsWith(".so"));

        if (libFiles != null) {
            for (File libFile : libFiles) {
                String dest = NATIVE_LIBRARIES_FOLDER + "/" + architecture + "/" + libFile.getName();
                getLog().debug("Adding " + libFile + " as " + dest);
                zipArchiver.addFile(libFile, dest);
            }
        }
    }

    /**
     * Generates an intermediate apk file (actually .ap_) containing the resources and assets.
     *
     * @throws MojoExecutionException if an error occurs while generating the intermediate apk file
     */
    private void generateIntermediateApk() throws MojoExecutionException {
        // Have to generate the AAR against the dependent resources or build will fail if any local resources
        // directly reference any of the dependent resources. NB this does NOT include the dep resources in the AAR.
        List<File> dependenciesResDirectories = new ArrayList<>();
        for (Artifact libraryArtifact : getTransitiveDependencyArtifacts(APKLIB, AAR)) {
            final File apkLibResDir = getUnpackedLibResourceFolder(libraryArtifact);
            if (apkLibResDir.exists()) {
                dependenciesResDirectories.add(apkLibResDir);
            }
        }

        final CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        File outputFile = new File(targetDirectory, finalName + ".ap_");

        final AaptCommandBuilder commandBuilder = AaptCommandBuilder
                .packageResources(getLog())
                .makePackageDirectories()
                .forceOverwriteExistingFiles()
                .setPathToAndroidManifest(destinationManifestFile)
                .addResourceDirectoriesIfExists(getResourceOverlayDirectories())
                .addResourceDirectoryIfExists(resourceDirectory)
                .addResourceDirectoriesIfExists(dependenciesResDirectories)
                .autoAddOverlay()
                .addRawAssetsDirectoryIfExists(combinedAssets)
                .addExistingPackageToBaseIncludeSet(getAndroidSdk().getAndroidJar())
                .setOutputApkFile(outputFile)
                .addConfigurations(configurations)
                .setResourceConstantsFolder(genDirectory)
                .makeResourcesNonConstant()
                .generateRTextFile(targetDirectory)
                .setVerbose(aaptVerbose);

        getLog().debug(getAndroidSdk().getAaptPath() + " " + commandBuilder.toString());
        getLog().info("Generating aar");
        try {
            executor.setCaptureStdOut(true);
            final List<String> commands = commandBuilder.build();
            executor.executeCommand(getAndroidSdk().getAaptPath(), commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }
    }
}
