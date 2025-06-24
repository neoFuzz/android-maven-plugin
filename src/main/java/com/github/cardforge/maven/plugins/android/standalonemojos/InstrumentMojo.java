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

import com.github.cardforge.maven.plugins.android.AbstractInstrumentationMojo;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;

import javax.inject.Inject;

/**
 * Runs the instrumentation apk on device.
 *
 * @author hugo.josefson@jayway.com
 */
@SuppressWarnings("unused") // used in Maven goals
@Mojo(name = "instrument")
public class InstrumentMojo extends AbstractInstrumentationMojo {

    /**
     * {@inheritDoc}
     */
    @Inject
    protected InstrumentMojo(ArtifactResolver artifactResolver, ArtifactHandler artHandler,
                             MavenProjectHelper projectHelper, DependencyGraphBuilder dependencyGraphBuilder) {
        super(artifactResolver, artHandler, projectHelper, dependencyGraphBuilder);
    }

    /**
     * @throws MojoExecutionException if the execution fails
     * @throws MojoFailureException   if the apk file does not exist
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        instrument();
    }

}
