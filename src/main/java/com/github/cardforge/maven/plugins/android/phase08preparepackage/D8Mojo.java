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
import com.github.cardforge.maven.plugins.android.configuration.D8;
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

/**
 * Converts compiled Java classes (including those containing Java 8 syntax) to the Android dex format.
 * It is a replacement for the {@link DexMojo}.
 * <p>
 * You should only run one or the other.
 * By default, D8 will run and Dex will not. But this is determined by the
 *
 * @author william.ferguson@xandar.com.au
 */
@Mojo(
        name = "d8",
        defaultPhase = LifecyclePhase.PREPARE_PACKAGE,
        requiresDependencyResolution = ResolutionScope.COMPILE
)
public class D8Mojo extends AbstractAndroidMojo {
    /**
     * The JAR file extension.
     */
    private static final String JAR = "jar";

    /**
     * The dex compiler to use. Allowed values are 'dex' (default) and 'd8'.
     */
    @Parameter(property = "android.dex.compiler", defaultValue = "dex")
    private String dexCompiler;

    /**
     * Configuration for the D8 command execution. It can be configured in the plugin configuration like so
     *
     * <pre>
     * &lt;dexCompiler&gt;d8&lt;/dexCompiler&gt;
     * &lt;d8&gt;
     *   &lt;jvmArguments&gt;
     *     &lt;jvmArgument&gt;-Xms256m&lt;/jvmArgument&gt;
     *     &lt;jvmArgument&gt;-Xmx512m&lt;/jvmArgument&gt;
     *   &lt;/jvmArguments&gt;
     *   &lt;intermediate&gt;true|false&lt;/intermediate&gt;
     *   &lt;mainDexList&gt;path to class list file&lt;/mainDexList&gt;
     *   &lt;release&gt;path to class list file&lt;/release&gt;
     *   &lt;minApi&gt;minimum API level compatibility&lt;/minApi&gt;
     *   &lt;arguments&gt;
     *     &lt;argument&gt;--opt1&lt;/argument&gt;
     *     &lt;argument&gt;value1A&lt;/argument&gt;
     *     &lt;argument&gt;--opt2&lt;/argument&gt;
     *   &lt;/arguments&gt;
     * &lt;/d8&gt;
     * </pre>
     * <p>
     * or via properties d8* or command line parameters android.d8.*
     */
    @Parameter
    private D8 d8;

    /**
     * Extra JVM Arguments. Using these you can e.g. increase memory for the jvm running the build.
     */
    @Parameter(property = "android.d8.jvmArguments", defaultValue = "-Xmx1024M")
    private String[] d8JvmArguments;

    /**
     * Decides whether to pass the --intermediate flag to d8.
     */
    @Parameter(property = "android.d8.intermediate", defaultValue = "false")
    private boolean d8Intermediate;

    /**
     * Full path to class list to multi dex
     */
    @Parameter(property = "android.d8.mainDexList")
    private String d8MainDexList;

    /**
     * Whether to pass the --release flag to d8.
     */
    @Parameter(property = "android.d8.release", defaultValue = "false")
    private boolean d8Release;

    /**
     * The minApi (if any) to pass to d8.
     */
    @Parameter(property = "android.d8.minApi")
    private Integer d8MinApi;

    /**
     * Additional command line parameters passed to d8.
     */
    @Parameter(property = "android.d8.arguments")
    private String[] d8Arguments;

    /**
     * The name of the obfuscated JAR.
     */
    @Parameter(property = "android.proguard.obfuscatedJar")
    private File obfuscatedJar;

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
    private boolean parsedIntermediate;
    private String parsedMainDexList;
    private String[] parsedArguments;
    private DexCompiler parsedDexCompiler;
    private boolean parsedRelease;
    private Integer parsedMinApi;

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
     *     <li>If the selected DEX compiler is not D8, skips execution with a log message.</li>
     *     <li>If APK generation is enabled, calls {@link #runD8(CommandExecutor)} to run the D8 compiler.</li>
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
        if (parsedDexCompiler != DexCompiler.D8) {
            getLog().info("Not executing D8Mojo because DEX compiler is set to " + parsedDexCompiler);
            return;
        }

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommandExecutor();
        executor.setLogger(getLog());

        if (generateApk) {
            runD8(executor);
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
     * @return the dependency list
     */
    @NonNull
    private List<File> getDependencies() {
        final List<File> libraries = new ArrayList<>();
        for (Artifact artifact : filterArtifacts(getTransitiveDependencyArtifacts(), skipDependencies,
                artifactTypeSet.getIncludes(), artifactTypeSet.getExcludes(), artifactSet.getIncludes(),
                artifactSet.getExcludes())) {
            if ("jar".equals(artifact.getType())) {
                libraries.add(artifact.getFile());
            }
        }

        return libraries;
    }

    /**
     * @return Set of input files for dex. This is a combination of directories and jar files.
     */
    @NonNull
    private Set<File> getD8InputFiles() {
        final Set<File> inputs = new HashSet<>();

        if (obfuscatedJar != null && obfuscatedJar.exists()) {
            // proguard has been run, use this jar
            getLog().debug("Adding dex input (obfuscatedJar) : " + obfuscatedJar);
            inputs.add(obfuscatedJar);
        } else {
            getLog().debug("Using non-obfuscated input");
            final File classesJar = new File(targetDirectory, finalName + ".jar");
            inputs.add(classesJar);
            getLog().debug("Adding dex input from : " + classesJar);

            for (Artifact artifact : filterArtifacts(getTransitiveDependencyArtifacts(), skipDependencies,
                    artifactTypeSet.getIncludes(), artifactTypeSet.getExcludes(), artifactSet.getIncludes(),
                    artifactSet.getExcludes())) {
                if (artifact.getType().equals(JAR)) {
                    getLog().debug("Adding dex input : " + artifact.getFile());
                    inputs.add(artifact.getFile().getAbsoluteFile());
                }
            }
        }

        return inputs;
    }

    /**
     * Parses the configuration settings for the D8 command execution.
     * It prioritizes the configuration settings in the following order:
     * 1. Configuration in the plugin configuration
     * 2. Configuration in the pom.xml
     * 3. Default values
     */
    private void parseConfiguration() {
        // config in pom found
        if (d8 != null) {
            // the if statements make sure that properties/command line
            // parameter overrides configuration
            // and that the defaults apply in all cases:
            if (d8.getJvmArguments() == null) {
                parsedJvmArguments = d8JvmArguments;
            } else {
                parsedJvmArguments = d8.getJvmArguments();
            }
            if (d8.isIntermediate() == null) {
                parsedIntermediate = d8Intermediate;
            } else {
                parsedIntermediate = d8.isIntermediate();
            }
            if (d8.getMainDexList() == null) {
                parsedMainDexList = d8MainDexList;
            } else {
                parsedMainDexList = d8.getMainDexList();
            }
            if (d8.getArguments() == null) {
                parsedArguments = d8Arguments;
            } else {
                parsedArguments = d8.getArguments();
            }
            parsedDexCompiler = DexCompiler.valueOfIgnoreCase(dexCompiler);
            if (d8.isRelease() == null) {
                parsedRelease = release;
            } else {
                parsedRelease = d8.isRelease();
            }
            if (d8.getMinApi() == null) {
                parsedMinApi = d8MinApi;
            } else {
                parsedMinApi = d8.getMinApi();
            }
        } else {
            parsedJvmArguments = d8JvmArguments;
            parsedIntermediate = d8Intermediate;
            parsedMainDexList = d8MainDexList;
            parsedArguments = d8Arguments;
            parsedDexCompiler = DexCompiler.valueOfIgnoreCase(dexCompiler);
            parsedRelease = d8Release;
            parsedMinApi = d8MinApi;
        }
    }

    /**
     * @return the default commands for the java process.
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
     * @param executor the command executor to use for executing the D8 command.
     * @throws MojoExecutionException If there is an error executing the D8 command.
     */
    private void runD8(CommandExecutor executor) throws MojoExecutionException {
        final List<String> commands = javaDefaultCommands();

        // Add d8 class to be invoked (As of Android 30 the D8 class is not included as a main attribute in the Jar).
        commands.add("-classpath");
        commands.add(getAndroidSdk().getD8JarPath());
        commands.add("com.android.tools.r8.D8");

        final Set<File> inputFiles = getD8InputFiles();
        if (parsedIntermediate) {
            commands.add("--intermediate");
        }
        if (parsedMainDexList != null) {
            commands.add("--main-dex-list");
            commands.add(parsedMainDexList);
        }
        if (parsedArguments != null) {
            for (String argument : parsedArguments) {
                commands.add(argument);
            }
        }

        if (parsedRelease) {
            commands.add("--release");
        }

        if (parsedMinApi != null) {
            commands.add("--min-api");
            commands.add(parsedMinApi.toString());
        }

        commands.add("--output");
        commands.add(targetDirectory.getAbsolutePath());

        final File androidJar = getAndroidSdk().getAndroidJar();
        commands.add("--lib");
        commands.add(androidJar.getAbsolutePath());

        // Add project classpath
        final List<File> dependencies = getDependencies();
        for (final File file : dependencies) {
            commands.add("--classpath");
            commands.add(file.getAbsolutePath());
        }

        for (File inputFile : inputFiles) {
            commands.add(inputFile.getAbsolutePath());
        }

        for (String c : commands) {
            getLog().info("Command: " + c);
        }

        getLog().info("[D8] Convert classes to Dex : " + targetDirectory);
        executeJava(commands, executor);
    }

    /**
     * @return the path to the java executable, either from the system's PATH or from the java.home system property.
     */
    public String getJavaExecutablePath() {
        // First, check if 'java' is in the system's PATH
        String path = System.getenv("PATH");

        // Check if 'java' executable is found in PATH (on Windows, we need to check for 'java.exe')
        if (path != null && (path.contains("java") || path.contains("java.exe"))) {
            // If 'java' is in PATH, we can just use "java"
            return "java";
        } else {
            // If not in PATH, fall back to the absolute path derived from java.home
            return getJavaExecutable().getAbsolutePath();
        }
    }

    /**
     * @param commands the list of commands to execute.
     * @param executor the command executor to use for executing the Java command.
     * @return the output of the executed command.
     * @throws MojoExecutionException if an error occurs while executing the Java command.
     */
    private String executeJava(@NonNull final List<String> commands, @NonNull CommandExecutor executor)
            throws MojoExecutionException {
        final String javaExecutable = getJavaExecutablePath();
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
     * Creates an .apksource file for the project.
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
    protected void addJavaResources(JarArchiver jarArchiver, @NonNull List<Resource> javaResources) {
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
