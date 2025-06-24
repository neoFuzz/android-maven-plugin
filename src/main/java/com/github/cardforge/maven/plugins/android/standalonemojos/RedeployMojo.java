/*
 * Copyright (C) 2009 Jayway AB
 * Copyright (C) 2007-2008 JVending Masa
 * Copyright (C) 2010 akwuinet A.G.
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

import com.github.cardforge.maven.plugins.android.AbstractAndroidMojo;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;

import javax.inject.Inject;

import static com.github.cardforge.maven.plugins.android.common.AndroidExtension.APK;

/**
 * Undeploys and then deploys (= redeploys) the apk(s) of the current project(s) to all
 * attached devices and emulators. Automatically skips other projects in a multi-module
 * build that do not use packaging apk without terminating.<br>
 *
 * @author clement.escoffier@akquinet.de
 * @author Manfred Moser - manfred@simpligility.com
 */
@SuppressWarnings("unused") // Maven goal
@Mojo(name = "redeploy", requiresDependencyResolution = ResolutionScope.RUNTIME)
public class RedeployMojo extends AbstractAndroidMojo {

    /**
     * {@inheritDoc}
     */
    @Inject
    protected RedeployMojo(ArtifactResolver artifactResolver, ArtifactHandler artHandler,
                           MavenProjectHelper projectHelper, DependencyGraphBuilder dependencyGraphBuilder) {
        super(artifactResolver, artHandler, projectHelper, dependencyGraphBuilder);
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (project.getPackaging().equals(APK)) {
            String packageToUndeploy = renameManifestPackage != null
                    ? renameManifestPackage
                    : extractPackageNameFromAndroidManifest(destinationManifestFile);
            if (StringUtils.isNotBlank(packageToUndeploy)) {
                undeployApk(packageToUndeploy);
            }
            deployBuiltApk();
        } else {
            getLog().info("Project packaging is not apk, skipping redeployment");
        }
    }

}
