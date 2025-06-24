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
package com.github.cardforge.maven.plugins.android.phase11preintegrationtest;

import com.github.cardforge.maven.plugins.android.AbstractInstrumentationMojo;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;

import javax.inject.Inject;

/**
 * Internal. Do not use.<br>
 * Called automatically when the lifecycle reaches phase <code>pre-integration-test</code>. Figures out whether to
 * call goals in this phase; and if so, calls <code>android:deploy-dependencies</code> and <code>android:deploy</code>.
 *
 * @author hugo.josefson@jayway.com
 */
@SuppressWarnings("unused") // if it works as documented
@Mojo(name = "internal-pre-integration-test", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class InternalPreIntegrationTestMojo extends AbstractInstrumentationMojo {

    /**
     * {@inheritDoc}
     */
    @Inject
    protected InternalPreIntegrationTestMojo(ArtifactResolver artifactResolver,
                                             ArtifactHandler artHandler,
                                             MavenProjectHelper projectHelper,
                                             DependencyGraphBuilder dependencyGraphBuilder) {
        super(artifactResolver, artHandler, projectHelper, dependencyGraphBuilder);
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (isEnableIntegrationTest()) {
            deployDependencies();
            deployBuiltApk();
        }
    }

}