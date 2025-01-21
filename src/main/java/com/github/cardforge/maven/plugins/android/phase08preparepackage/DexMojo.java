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
package com.github.cardforge.maven.plugins.android.phase08preparepackage;

import com.android.annotations.NonNull;
import com.github.cardforge.maven.plugins.android.AbstractAndroidMojo;
import com.github.cardforge.maven.plugins.android.CommandExecutor;
import com.github.cardforge.maven.plugins.android.ExecutionException;
import com.github.cardforge.maven.plugins.android.IncludeExcludeSet;
import com.github.cardforge.maven.plugins.android.common.Const;
import com.github.cardforge.maven.plugins.android.common.ZipExtractor;
import com.github.cardforge.maven.plugins.android.configuration.Dex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.cardforge.maven.plugins.android.InclusionExclusionResolver.filterArtifacts;
import static com.github.cardforge.maven.plugins.android.common.AndroidExtension.*;

/**
 * Converts compiled Java classes to the Android dex format.
 * <p>
 * Configuration for the dex command execution. It can be configured in the plugin configuration like so
 *
 * <pre>
 * &lt;dexCompiler&gt;dex&lt;/dexCompiler&gt;
 * &lt;dex&gt;
 *   &lt;jvmArguments&gt;
 *     &lt;jvmArgument&gt;-Xms256m&lt;/jvmArgument&gt;
 *     &lt;jvmArgument&gt;-Xmx512m&lt;/jvmArgument&gt;
 *   &lt;/jvmArguments&gt;
 *   &lt;coreLibrary&gt;true|false&lt;/coreLibrary&gt;
 *   &lt;noLocals&gt;true|false&lt;/noLocals&gt;
 *   &lt;forceJumbo&gt;true|false&lt;/forceJumbo&gt;
 *   &lt;optimize&gt;true|false&lt;/optimize&gt;
 *   &lt;preDex&gt;true|false&lt;/preDex&gt;
 *   &lt;preDexLibLocation&gt;path to predexed libraries, defaults to target/dexedLibs&lt;/preDexLibLocation&gt;
 *   &lt;incremental&gt;true|false&lt;/incremental&gt;
 *   &lt;multiDex&gt;true|false&lt;/multiDex&gt;
 *   &lt;generateMainDexList&gt;true|false&lt;/generateMainDexList&gt;
 *   &lt;mainDexList&gt;path to class list file&lt;/mainDexList&gt;
 *   &lt;minimalMainDex&gt;true|false&lt;/minimalMainDex&gt;
 * &lt;/dex&gt;
 * </pre>
 * <p>
 * or via properties dex.* or command line parameters android.dex.*
 *
 * @author hugo.josefson@jayway.com
 */
@Mojo(
        name = "dex",
        defaultPhase = LifecyclePhase.PREPARE_PACKAGE,
        requiresDependencyResolution = ResolutionScope.COMPILE
)
public class DexMojo extends AbstractAndroidMojo {
    /**
     * The dex compiler to use. Allowed values are 'dex' (default) and 'd8'.
     */
    @Parameter(property = "android.dex.compiler", defaultValue = "dex")
    private String dexCompiler;

    @Parameter
    private Dex dex;

    /**
     * Extra JVM Arguments. Using these you can e.g. increase memory for the jvm running the build.
     */
    @Parameter(property = "android.dex.jvmArguments", defaultValue = "-Xmx1024M")
    private String[] dexJvmArguments;

    /**
     * Decides whether to pass the --core-library flag to dx.
     */
    @Parameter(property = "android.dex.coreLibrary", defaultValue = "false")
    private boolean dexCoreLibrary;

    /**
     * Decides whether to pass the --no-locals flag to dx.
     */
    @Parameter(property = "android.dex.noLocals", defaultValue = "false")
    private boolean dexNoLocals;

    /**
     * Decides whether to pass the --no-optimize flag to dx.
     */
    @Parameter(property = "android.dex.optimize", defaultValue = "true")
    private boolean dexOptimize;

    /**
     * Decides whether to predex the jars.
     */
    @Parameter(property = "android.dex.predex", defaultValue = "false")
    private boolean dexPreDex;

    /**
     * Decides whether to use force jumbo mode.
     */
    @Parameter(property = "android.dex.forcejumbo", defaultValue = "false")
    private boolean dexForceJumbo;

    /**
     * Path to predexed libraries.
     */
    @Parameter(
            property = "android.dex.dexPreDexLibLocation",
            defaultValue = "${project.build.directory}${file.separator}dexedLibs"
    )
    private String dexPreDexLibLocation;

    /**
     * Decides whether to pass the --incremental flag to dx.
     */
    @Parameter(property = "android.dex.incremental", defaultValue = "false")
    private boolean dexIncremental;

    /**
     * The name of the obfuscated JAR.
     */
    @Parameter(property = "android.proguard.obfuscatedJar")
    private File obfuscatedJar;

    /**
     * Decides whether to pass the --multi-dex flag to dx.
     */
    @Parameter(property = "android.dex.multidex", defaultValue = "false")
    private boolean dexMultiDex;

    /**
     * Full path to class list to multi dex
     */
    @Parameter(property = "android.dex.maindexlist")
    private String dexMainDexList;

    /**
     * Decides whether to pass the --minimal-main-dex flag to dx.
     */
    @Parameter(property = "android.dex.minimalmaindex", defaultValue = "false")
    private boolean dexMinimalMainDex;

    /**
     * Decides whether to generate main dex list.
     * Supported from build tools version 22.0.0+
     * <p>
     * Note: if set to true, dexMinimalMainDex is set to true, and dexMainDexList
     * is set to generated main dex list.
     */
    @Parameter(property = "android.dex.generatemaindexlist", defaultValue = "false")
    private boolean dexGenerateMainDexList;

    /**
     * Additional command line parameters passed to dx.
     */
    @Parameter(property = "android.dex.dexarguments")
    private String dexArguments;

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

    private String[] parsedJvmArguments;
    private boolean parsedCoreLibrary;
    private boolean parsedNoLocals;
    private boolean parsedOptimize;
    private boolean parsedPreDex;
    private boolean parsedForceJumbo;
    private String parsedPreDexLibLocation;
    private boolean parsedIncremental;
    private boolean parsedMultiDex;
    private String parsedMainDexList;
    private boolean parsedMinimalMainDex;
    private boolean parsedGenerateMainDexList;
    private String parsedDexArguments;
    private DexCompiler parsedDexCompiler;

    /**
     * Figure out the full path to the current java executable.
     *
     * @return the full path to the current java executable.
     */
    @NonNull
    private static File getJavaExecutable() {
        final String javaHome = System.getProperty("java.home");
        final String slash = File.separator;
        return new File(javaHome + slash + "bin" + slash + "java");
    }

    /**
     * Executes the Mojo goal by performing the following tasks:
     * <ul>
     *     <li>Parses the configuration settings.</li>
     *     <li>If the selected DEX compiler is not DEX, skips execution with a log message.</li>
     *     <li>If APK generation is enabled, calls {@link #runDex} to run the DEX compiler.</li>
     *     <li>If the attachment of JAR is enabled, attaches the generated JAR file to the Maven project.</li>
     *     <li>If attaching sources is enabled, creates and attaches an APK sources file.</li>
     * </ul>
     * <p>
     * This method is typically invoked during the execution of a Maven build to handle APK compilation and artifact attachment tasks.
     * </p>
     *
     * @throws MojoExecutionException If there is an error executing the Mojo.
     * @throws MojoFailureException   If the Mojo fails due to a configuration or other critical issue.
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        parseConfiguration();

        getLog().debug("DexCompiler set to " + parsedDexCompiler);
        if (parsedDexCompiler != DexCompiler.DEX) {
            getLog().info("Not executing DexMojo because DEX compiler is set to " + parsedDexCompiler);
            return;
        }

        if (getJack().isEnabled()) {
            //Dexxing is handled by Jack
            return;
        }

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(getLog());

        File outputFile;
        if (parsedMultiDex) {
            outputFile = targetDirectory;
            if (parsedGenerateMainDexList) {
                getAndroidSdk().assertThatBuildToolsVersionIsAtLeast(
                        "22.0.0", "generate main dex list");
                File generatedMainDexClassesList = generateMainDexClassesList(executor);
                parsedMainDexList = generatedMainDexClassesList.getAbsolutePath();
                parsedMinimalMainDex = true;
            }
        } else {
            outputFile = new File(targetDirectory, "classes.dex");
        }
        if (generateApk) {
            runDex(executor, outputFile);
        }

        if (attachJar) {
            File jarFile = new File(targetDirectory + File.separator
                    + finalName + ".jar");
            projectHelper.attachArtifact(project, "jar", project.getArtifact().getClassifier(), jarFile);
        }

        if (attachSources) {
            // Also attach an .apksources, containing sources from this project.
            final File apksources = createApkSourcesFile();
            projectHelper.attachArtifact(project, "apksources", apksources);
        }
    }

    /**
     * Retrieves the set of input files to be processed by the dex tool.
     * <p>
     * This method determines the appropriate input files based on whether ProGuard obfuscation has been applied.
     * If an obfuscated JAR file exists, it is added to the input set. Otherwise, the original project output
     * directory and its dependencies are used.
     * <p>
     * The method also filters out certain artifact types such as native dependencies and handles APK and AAR
     * dependencies appropriately.
     *
     * @return A set of {@link File} objects representing the input files for the dex tool.
     * @throws MojoExecutionException if an error occurs while determining the input files.
     */
    @NonNull
    private Set<File> getDexInputFiles() throws MojoExecutionException {
        Set<File> inputs = new HashSet<>();

        if (obfuscatedJar != null && obfuscatedJar.exists()) {
            // proguard has been run, use this jar
            getLog().debug("Adding dex input (obfuscatedJar) : " + obfuscatedJar);
            inputs.add(obfuscatedJar);
        } else {
            getLog().debug("Using non-obfuscated input");
            // no proguard, use original config
            inputs.add(projectOutputDirectory);
            getLog().debug("Adding dex input : " + project.getBuild().getOutputDirectory());
            for (Artifact artifact : filterArtifacts(getTransitiveDependencyArtifacts(), skipDependencies,
                    artifactTypeSet.getIncludes(), artifactTypeSet.getExcludes(), artifactSet.getIncludes(),
                    artifactSet.getExcludes())) {
                if (artifact.getType().equals(Const.ArtifactType.NATIVE_SYMBOL_OBJECT)
                        || artifact.getType().equals(Const.ArtifactType.NATIVE_IMPLEMENTATION_ARCHIVE)) {
                    // Ignore native dependencies - no need for dexer to see those
                } else if (artifact.getType().equals(APKLIB)) {
                    // Any jars in the libs folder should now be
                    // automatically included because they will be a transitive dependency.
                } else if (artifact.getType().equals(AAR)) {
                    // The Aar classes.jar should now be automatically included
                    // because it will be a transitive dependency. As should any jars in the libs folder.
                } else if (artifact.getType().equals(APK)) {
                    // We need to dex the APK classes including the APK R.
                    // But we don't want to add a second instance of the embedded Rs for any of the APK's dependencies
                    // as they will already have been generated to target/classes. The R values from the APK will be
                    // the correct ones, so the best solution is to extract the APK classes (including all Rs) to
                    // target/classes overwriting any generated Rs and let dex pick up the values from there.
                    getLog().debug("Extracting APK classes to target/classes : " + artifact.getArtifactId());
                    final File apkClassesJar = getUnpackedLibHelper().getJarFileForApk(artifact);
                    getLog().debug("Extracting APK : " + apkClassesJar + " to " + targetDirectory);
                    final ZipExtractor extractor = new ZipExtractor(getLog());
                    extractor.extract(apkClassesJar, targetDirectory, ".class");
                } else {
                    getLog().debug("Adding dex input : " + artifact.getFile());
                    inputs.add(artifact.getFile().getAbsoluteFile());
                }
            }
        }

        return inputs;
    }

    /**
     * Parses the configuration settings for the DEX compilation process.
     * This method sets various configuration options based on the provided configuration
     * and default values.
     */
    private void parseConfiguration() {
        // config in pom found
        if (dex != null) {
            // the if statements make sure that properties/command line
            // parameter overrides configuration
            // and that the dafaults apply in all cases
            if (dex.getJvmArguments() == null) {
                parsedJvmArguments = dexJvmArguments;
            } else {
                parsedJvmArguments = dex.getJvmArguments();
            }
            if (dex.isCoreLibrary() == null) {
                parsedCoreLibrary = dexCoreLibrary;
            } else {
                parsedCoreLibrary = dex.isCoreLibrary();
            }
            if (dex.isNoLocals() == null) {
                parsedNoLocals = dexNoLocals;
            } else {
                parsedNoLocals = dex.isNoLocals();
            }
            if (dex.isOptimize() == null) {
                parsedOptimize = dexOptimize;
            } else {
                parsedOptimize = dex.isOptimize();
            }
            if (dex.isPreDex() == null) {
                parsedPreDex = dexPreDex;
            } else {
                parsedPreDex = dex.isPreDex();
            }
            if (dex.getPreDexLibLocation() == null) {
                parsedPreDexLibLocation = dexPreDexLibLocation;
            } else {
                parsedPreDexLibLocation = dex.getPreDexLibLocation();
            }
            if (dex.isIncremental() == null) {
                parsedIncremental = dexIncremental;
            } else {
                parsedIncremental = dex.isIncremental();
            }
            if (dex.isForceJumbo() == null) {
                parsedForceJumbo = dexForceJumbo;
            } else {
                parsedForceJumbo = dex.isForceJumbo();
            }
            if (dex.isMultiDex() == null) {
                parsedMultiDex = dexMultiDex;
            } else {
                parsedMultiDex = dex.isMultiDex();
            }
            if (dex.getMainDexList() == null) {
                parsedMainDexList = dexMainDexList;
            } else {
                parsedMainDexList = dex.getMainDexList();
            }
            if (dex.isMinimalMainDex() == null) {
                parsedMinimalMainDex = dexMinimalMainDex;
            } else {
                parsedMinimalMainDex = dex.isMinimalMainDex();
            }
            if (dex.isGenerateMainDexList() == null) {
                parsedGenerateMainDexList = dexGenerateMainDexList;
            } else {
                parsedGenerateMainDexList = dex.isGenerateMainDexList();
            }
            if (dex.getDexArguments() == null) {
                parsedDexArguments = dexArguments;
            } else {
                parsedDexArguments = dex.getDexArguments();
            }
            parsedDexCompiler = DexCompiler.valueOfIgnoreCase(dexCompiler);

        } else {
            parsedJvmArguments = dexJvmArguments;
            parsedCoreLibrary = dexCoreLibrary;
            parsedNoLocals = dexNoLocals;
            parsedOptimize = dexOptimize;
            parsedPreDex = dexPreDex;
            parsedPreDexLibLocation = dexPreDexLibLocation;
            parsedIncremental = dexIncremental;
            parsedForceJumbo = dexForceJumbo;
            parsedMultiDex = dexMultiDex;
            parsedMainDexList = dexMainDexList;
            parsedMinimalMainDex = dexMinimalMainDex;
            parsedGenerateMainDexList = dexGenerateMainDexList;
            parsedDexArguments = dexArguments;
            parsedDexCompiler = DexCompiler.valueOfIgnoreCase(dexCompiler);
        }
    }

    /**
     * Pre-dexes the input files if pre-dexing is enabled.
     *
     * @param executor   The command executor to use for executing the pre-dexing process.
     * @param inputFiles The set of input files to be pre-dexed.
     * @return The set of pre-dexed files.
     * @throws MojoExecutionException If an error occurs during the pre-dexing process.
     */
    @NonNull
    private Set<File> preDex(CommandExecutor executor, @NonNull Set<File> inputFiles) throws MojoExecutionException {
        Set<File> filtered = new HashSet<>();
        getLog().info("Pre dex-ing libraries for faster dex-ing of the final application.");

        for (File inputFile : inputFiles) {
            if (inputFile.getName().matches(".*\\.jar$")) {
                List<String> commands = dexDefaultCommands();

                File predexJar = predexJarPath(inputFile);
                commands.add("--output=" + predexJar.getAbsolutePath());
                commands.add(inputFile.getAbsolutePath());
                filtered.add(predexJar);

                if (!predexJar.isFile() || predexJar.lastModified() < inputFile.lastModified()) {
                    getLog().info("Pre-dex ing jar: " + inputFile.getAbsolutePath());
                    executeJava(commands, executor);
                }
            } else {
                filtered.add(inputFile);
            }
        }

        return filtered;
    }

    /**
     * @param inputFile The input file to create the pre-dexed JAR file for.
     * @return The pre-dexed JAR file.
     */
    @NonNull
    private File predexJarPath(@NonNull File inputFile) {
        final File predexLibsDirectory = new File(parsedPreDexLibLocation.trim());
        predexLibsDirectory.mkdirs();
        return new File(predexLibsDirectory, inputFile.getName());
    }

    /**
     * @return The commands returned are in the form of a list that can be passed to
     * {@link CommandExecutor#executeCommand(Runnable)}.
     * @throws MojoExecutionException if an error occurs while creating the commands.
     */
    @NonNull
    private List<String> dexDefaultCommands() throws MojoExecutionException {
        List<String> commands = jarDefaultCommands();
        commands.add(getAndroidSdk().getDxJarPath());
        commands.add("--dex");
        return commands;
    }

    /**
     * @return The commands returned are in the form of a list that can be passed to
     * {@link CommandExecutor#executeCommand}.
     */
    @NonNull
    private List<String> jarDefaultCommands() {
        List<String> commands = javaDefaultCommands();
        commands.add("-jar");
        return commands;
    }

    /**
     * @return The commands returned are in the form of a list that can be passed to
     * {@link CommandExecutor#executeCommand}.
     */
    @NonNull
    private List<String> javaDefaultCommands() {
        List<String> commands = new ArrayList<>();
        if (parsedJvmArguments != null) {
            for (String jvmArgument : parsedJvmArguments) {
                // preserve backward compatibility allowing argument with or
                // without dash (e.g. Xmx512m as well as
                // -Xmx512m should work) (see
                // http://code.google.com/p/maven-android-plugin/issues/detail?id=153)
                if (!jvmArgument.startsWith("-")) {
                    jvmArgument = "-" + jvmArgument;
                }
                getLog().debug("Adding jvm argument " + jvmArgument);
                commands.add(jvmArgument);
            }
        }
        return commands;
    }

    /**
     * Runs the dexing process on the input files.
     *
     * @param executor   The command executor to use for executing the dexing process.
     * @param outputFile The output file to write the dexed classes to.
     * @throws MojoExecutionException if an error occurs during the dexing process.
     */
    private void runDex(CommandExecutor executor, File outputFile)
            throws MojoExecutionException {
        final List<String> commands = dexDefaultCommands();
        final Set<File> inputFiles = getDexInputFiles();
        Set<File> filteredFiles = inputFiles;
        if (parsedPreDex) {
            filteredFiles = preDex(executor, inputFiles);
        }
        if (!parsedOptimize) {
            commands.add("--no-optimize");
        }
        if (parsedCoreLibrary) {
            commands.add("--core-library");
        }
        if (parsedIncremental) {
            commands.add("--incremental");
        }
        if (parsedNoLocals) {
            commands.add("--no-locals");
        }
        if (parsedForceJumbo) {
            commands.add("--force-jumbo");
        }
        if (parsedMultiDex) {
            commands.add("--multi-dex");
            if (parsedMainDexList != null) {
                commands.add("--main-dex-list=" + parsedMainDexList);
            }
            if (parsedMinimalMainDex) {
                commands.add("--minimal-main-dex");
            }
        }
        if (parsedDexArguments != null) {
            commands.add(parsedDexArguments);
        }
        commands.add("--output=" + outputFile.getAbsolutePath());
        for (File inputFile : filteredFiles) {
            commands.add(inputFile.getAbsolutePath());
        }

        getLog().info("Convert classes to Dex : " + outputFile);
        executeJava(commands, executor);
    }

    /**
     * @param commands The commands to execute.
     * @param executor The command executor to use for executing the commands.
     * @return The output of the executed commands.
     * @throws MojoExecutionException if an error occurs during the execution of the commands.
     */
    private String executeJava(@NonNull final List<String> commands, @NonNull CommandExecutor executor) throws MojoExecutionException {
        final String javaExecutable = getJavaExecutable().getAbsolutePath();
        getLog().debug(javaExecutable + " " + commands);
        try {
            executor.setCaptureStdOut(true);
            executor.executeCommand(javaExecutable, commands, project.getBasedir(), false);
            return executor.getStandardOut();
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }
    }

    /**
     * @param executor The command executor to use for executing the commands.
     * @return The main dex classes jar file.
     * @throws MojoExecutionException if an error occurs during the generation of the main dex classes jar.
     */
    @NonNull
    private File generateMainDexClassesJar(CommandExecutor executor) throws MojoExecutionException {
        List<String> commands = jarDefaultCommands();
        commands.add(getAndroidSdk().getProguardJarPath());
        commands.add("-dontnote");
        commands.add("-dontwarn");
        commands.add("-forceprocessing");
        commands.add("-dontoptimize");
        commands.add("-dontpreverify");
        commands.add("-dontobfuscate");

        Set<File> inputFiles = getDexInputFiles();
        for (File inputFile : inputFiles) {
            commands.add("-injars");
            commands.add(inputFile.getAbsolutePath() + "(!META-INF/**)");
        }

        commands.add("-libraryjars");
        commands.add(getAndroidSdk().getShrinkedAndroidJarPath());

        commands.add("-include");
        commands.add(getAndroidSdk().getMainDexClassesRulesPath());

        commands.add("-outjars");
        File mainDexClassesJar = new File(targetDirectory, "mainDexClasses.jar");
        commands.add(mainDexClassesJar.getAbsolutePath());

        getLog().info("Generating main dex classes jar : " + mainDexClassesJar);
        executeJava(commands, executor);

        return mainDexClassesJar;
    }

    /**
     * @param executor The command executor to use for executing the commands.
     * @return The main dex classes list file.
     * @throws MojoExecutionException if an error occurs during the generation of the main dex classes list.
     */
    @NonNull
    private File generateMainDexClassesList(CommandExecutor executor) throws MojoExecutionException {
        File mainDexClassesJar = generateMainDexClassesJar(executor);
        List<String> commands = javaDefaultCommands();

        commands.add("-Djava.ext.dirs=" + getAndroidSdk().getBuildToolsLibDirectoryPath());
        commands.add("com.android.multidex.MainDexListBuilder");
        commands.add(mainDexClassesJar.getAbsolutePath());

        Set<File> inputFiles = getDexInputFiles();
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.join(inputFiles, File.pathSeparatorChar));
        commands.add(sb.toString());

        File mainDexClassesList = new File(targetDirectory, "mainDexClasses.txt");

        getLog().info("Generating main dex classes list : " + mainDexClassesList);

        String output = executeJava(commands, executor);
        try {
            FileUtils.writeStringToFile(mainDexClassesList, output);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write command output with main dex classes list to "
                    + mainDexClassesList, ex);
        }
        return mainDexClassesList;
    }

    /**
     * Create the {@code .apksource} file for the project.
     *
     * @return the {@link File} for the .apksource file to attach to the project.
     * @throws MojoExecutionException if an error occurs while creating the .apksource file.
     */
    protected File createApkSourcesFile() throws MojoExecutionException {
        final File apksources = new File(targetDirectory, finalName
                + ".apksources");
        FileUtils.deleteQuietly(apksources);

        try {
            JarArchiver jarArchiver = new JarArchiver();
            jarArchiver.setDestFile(apksources);

            addDirectory(jarArchiver, assetsDirectory, "assets");
            addDirectory(jarArchiver, resourceDirectory, "res");
            addDirectory(jarArchiver, sourceDirectory, "src/main/java");
            addJavaResources(jarArchiver, resources);

            jarArchiver.createArchive();
        } catch (ArchiverException e) {
            throw new MojoExecutionException("ArchiverException while creating .apksource file.", e);
        } catch (IOException e) {
            throw new MojoExecutionException("IOException while creating .apksource file.", e);
        }

        return apksources;
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
     * @param jarArchiver The {@link JarArchiver} to add the directory to.
     * @param directory   The directory to add.
     * @param prefix      An optional prefix for where in the Jar file the directory's contents should go.
     */
    protected void addDirectory(JarArchiver jarArchiver, File directory, String prefix) {
        if (directory != null && directory.exists()) {
            final DefaultFileSet fileSet = new DefaultFileSet();
            fileSet.setPrefix(endWithSlash(prefix));
            fileSet.setDirectory(directory);
            jarArchiver.addFileSet(fileSet);
        }
    }

    /**
     * Adds a list of Java resources to a {@link JarArchiver}.
     *
     * @param jarArchiver   The {@link JarArchiver} to add the resources to.
     * @param javaResources The list of Java resources to add.
     */
    protected void addJavaResources(@NonNull JarArchiver jarArchiver, @NonNull List<Resource> javaResources) {
        for (Resource javaResource : javaResources) {
            addJavaResource(jarArchiver, javaResource);
        }
    }

    /**
     * Adds a Java Resources directory (typically "src/main/resources") to a {@link JarArchiver}.
     *
     * @param jarArchiver  The {@link JarArchiver} to add the resources to.
     * @param javaResource The Java resource to add.
     */
    protected void addJavaResource(JarArchiver jarArchiver, Resource javaResource) {
        if (javaResource != null) {
            final File javaResourceDirectory = new File(javaResource.getDirectory());
            if (javaResourceDirectory.exists()) {
                final DefaultFileSet javaResourceFileSet = new DefaultFileSet();
                javaResourceFileSet.setDirectory(javaResourceDirectory);
                javaResourceFileSet.setPrefix(endWithSlash("src/main/resources"));
                jarArchiver.addFileSet(javaResourceFileSet);
            }
        }
    }
}
