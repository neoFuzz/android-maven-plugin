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

package com.github.cardforge.maven.plugins.android.standalonemojos;

import com.android.annotations.NonNull;
import com.github.cardforge.maven.plugins.android.AbstractAndroidMojo;
import com.github.cardforge.maven.plugins.android.CommandExecutor;
import com.github.cardforge.maven.plugins.android.common.JarHelper;
import com.github.cardforge.maven.plugins.android.config.ConfigPojo;
import com.github.cardforge.maven.plugins.android.config.PullParameter;
import com.github.cardforge.maven.plugins.android.configuration.MetaInf;
import com.github.cardforge.maven.plugins.android.configuration.Unpack;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Unpack libraries code and dependencies into target.
 * <p>
 * This can be useful for using the proguard maven plugin to provide the input jars. Although it is encouraged to use
 * the proguard mojo of the android maven plugin.
 *
 * @author hugo.josefson@jayway.com
 * @author Manfred Moser
 */
@Mojo(name = "unpack", requiresDependencyResolution = ResolutionScope.COMPILE)
public class UnpackMojo extends AbstractAndroidMojo {
    /**
     * Static string
     */
    public static final String INTO = " into ";
    /**
     * If true, the library will be unpacked only when outputDirectory doesn't
     * exist, i.e, a clean build for most cases.
     *
     * @deprecated use ${android.unpack.lazy}
     */
    @Deprecated(since = "4.8", forRemoval = false)
    @Parameter(property = "android.lazyLibraryUnpack")
    private boolean lazyLibraryUnpack;

    @Parameter(property = "android.unpack.lazy")
    @PullParameter(defaultValueGetterMethod = "getLazyLibraryUnpack")
    private Boolean unpackLazy;

    @Parameter(alias = "metaInf")
    private MetaInf pluginMetaInf;

    @Parameter
    @ConfigPojo(prefix = "unpack")
    private Unpack unpack;

    @Parameter(defaultValue = "${project.build.directory}/android-classes", readonly = true)
    private File unpackOutputDirectory;

    @Parameter(defaultValue = "false", readonly = true)
    private boolean includeNonClassFiles;

    /**
     * {@inheritDoc}
     */
    protected UnpackMojo(ArtifactResolver artifactResolver, ArtifactHandler artHandler,
                         MavenProjectHelper projectHelper, DependencyGraphBuilder dependencyGraphBuilder) {
        super(artifactResolver, artHandler, projectHelper, dependencyGraphBuilder);
    }

    public void execute() throws MojoExecutionException, MojoFailureException {

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommandExecutor();
        executor.setLogger(this.getLog());

        if (generateApk) {
            // Unpack all dependent and main classes
            unpackClasses();
        }
    }

    private File unpackClasses() throws MojoExecutionException {
        File outputDirectory = unpackOutputDirectory;
        if (lazyLibraryUnpack && outputDirectory.exists()) {
            getLog().info("skip library unpacking due to lazyLibraryUnpack policy");
        } else {
            outputDirectory.mkdirs();

            for (Artifact artifact : getRelevantCompileArtifacts()) {
                processArtifacts(artifact, outputDirectory);
            }
        }

        try {
            if (!projectOutputDirectory.equals(outputDirectory)) {
                FileUtils.copyDirectory(projectOutputDirectory, outputDirectory);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("IOException while copying " + sourceDirectory.getAbsolutePath()
                    + INTO + outputDirectory.getAbsolutePath(), e);
        }
        return outputDirectory;
    }

    private void processArtifacts(@NonNull Artifact artifact, File outputDirectory) throws MojoExecutionException {
        if (artifact.getFile().isDirectory()) {
            try {
                FileUtils.copyDirectory(artifact.getFile(), outputDirectory);
            } catch (IOException e) {
                throw new MojoExecutionException("IOException while copying "
                        + artifact.getFile().getAbsolutePath() + INTO + outputDirectory.getAbsolutePath()
                        , e);
            }
        } else {
            try {
                JarHelper.unjar(new JarFile(artifact.getFile()), outputDirectory,
                        jarEntry -> isIncluded(jarEntry));
            } catch (IOException e) {
                throw new MojoExecutionException("IOException while unjarring "
                        + artifact.getFile().getAbsolutePath() + INTO + outputDirectory.getAbsolutePath()
                        , e);
            }
        }
    }

    boolean isIncluded(@NonNull JarEntry jarEntry) {
        String entName = jarEntry.getName();

        if (entName.endsWith(".class")) {
            return true;
        }

        return includeNonClassFiles && !entName.startsWith("META-INF/");
    }

    boolean getLazyLibraryUnpack() {
        return this.lazyLibraryUnpack;
    }
}
