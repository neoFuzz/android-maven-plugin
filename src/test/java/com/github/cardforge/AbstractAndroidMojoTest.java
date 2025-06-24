/*
 * Copyright (C) 2009, 2010 Jayway AB
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
package com.github.cardforge;

import com.android.ddmlib.DdmPreferences;
import com.github.cardforge.maven.plugins.android.AbstractAndroidMojo;
import com.github.cardforge.maven.plugins.android.AndroidSdk;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author hugo.josefson@jayway.com
 */
public class AbstractAndroidMojoTest {
    public static final String BASE_PATH = "maven/plugins/android/";
    protected AbstractAndroidMojo androidMojo;
    @Mock
    private ArtifactResolver mockArtifactResolver;
    @Mock
    private ArtifactHandler mockArtifactHandler;
    @Mock
    private MavenProjectHelper mockProjectHelper;
    @Mock
    private DependencyGraphBuilder mockDependencyGraphBuilder;

    @Before
    public void setUp() {
        androidMojo = new DefaultTestAndroidMojo(mockArtifactResolver, mockArtifactHandler,
                mockProjectHelper, mockDependencyGraphBuilder);
    }

    @Test
    public void givenNoPathThenUseAndroidHomePath() throws Exception {
        SdkTestSupport testSupport = new SdkTestSupport();
        androidMojo = new EmptyAndroidMojo(
                mockArtifactResolver,mockArtifactHandler,mockProjectHelper,mockDependencyGraphBuilder);

        setField(androidMojo, "sdkPath", null);
        setField(androidMojo, "sdkPlatform", "19");

        AndroidSdk sdk = androidMojo.getAndroidSdk();
        File path = (File) getField(sdk, "sdkPath");
        Assert.assertEquals(new File(testSupport.getenvAndroidHome()).getAbsolutePath(), path.getAbsolutePath());
    }

    @Test
    public void givenAndroidManifestThenTargetPackageIsFound() throws URISyntaxException {
        final URL url = this.getClass().getResource(BASE_PATH + "AndroidManifest.xml");
        final URI uri = url.toURI();
        final File file = new File(uri);
        final String foundTargetPackage = androidMojo.extractPackageNameFromAndroidManifest(file);
        Assert.assertEquals("com.example.android.apis.tests", foundTargetPackage);
    }

    @Test
    public void givenAndroidManifestThenInstrumentationRunnerIsFound()
            throws URISyntaxException, Exception {
        final URL url = this.getClass().getResource(BASE_PATH + "AndroidManifest.xml");
        final URI uri = url.toURI();
        final File file = new File(uri);
        final String foundInstrumentationRunner = androidMojo.extractInstrumentationRunnerFromAndroidManifest(file);
        Assert.assertEquals("android.test.InstrumentationTestRunner", foundInstrumentationRunner);
    }

    @Test
    public void givenAndroidManifestWithoutInstrumentationThenInstrumentationRunnerIsNotFound()
            throws URISyntaxException, MojoExecutionException {
        final URL url = this.getClass().getResource(BASE_PATH + "AndroidManifestWithoutInstrumentation.xml");
        final URI uri = url.toURI();
        final File file = new File(uri);
        final String foundInstrumentationRunner = androidMojo.extractInstrumentationRunnerFromAndroidManifest(file);
        Assert.assertNull(foundInstrumentationRunner);
    }

    @Test
    public void givenValidAndroidManifestXmlTreeThenPackageIsFound() throws IOException {
        final URL resource = this.getClass().getResource(BASE_PATH + "AndroidManifestXmlTree.txt");
        final InputStream inputStream = resource.openStream();
        final String androidManifestXmlTree = IOUtils.toString(inputStream);
        final String foundPackage = androidMojo.extractPackageNameFromAndroidManifestXmlTree(androidManifestXmlTree);
        Assert.assertEquals("com.example.android.apis", foundPackage);
    }

    @Test
    public void givenApidemosApkThenPackageIsFound() throws Exception {
        final URL resource = this.getClass().getResource(BASE_PATH + "apidemos-0.1.0-SNAPSHOT.apk");
        final String foundPackage = androidMojo.extractPackageNameFromApk(new File(new URI(resource.toString())));
        Assert.assertEquals("com.example.android.apis", foundPackage);
    }

    @Test
    public void givenApidemosPlatformtestsApkThenPackageIsFound() throws Exception {
        final URL resource = this.getClass().getResource(BASE_PATH + "apidemos-platformtests-0.1.0-SNAPSHOT.apk");
        final String foundPackage = androidMojo.extractPackageNameFromApk(new File(new URI(resource.toString())));
        Assert.assertEquals("com.example.android.apis.tests", foundPackage);
    }

    @Test
    public void usesAdbConnectionTimeout() throws MojoExecutionException {
        final int expectedTimeout = 1000;
        androidMojo.setAdbConnectionTimeout(expectedTimeout);
        androidMojo.initAndroidDebugBridge();

        Assert.assertEquals(DdmPreferences.getTimeOut(), expectedTimeout);
    }

    // Utility to set a private field value using reflection
    private void setField(@Nonnull Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // Utility to get a private field value using reflection
    private Object getField(@Nonnull Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private class DefaultTestAndroidMojo extends AbstractAndroidMojo {

        @Inject
        protected DefaultTestAndroidMojo(ArtifactResolver artifactResolver, ArtifactHandler artHandler, MavenProjectHelper projectHelper, DependencyGraphBuilder dependencyGraphBuilder) {
            super(artifactResolver, artHandler, projectHelper, dependencyGraphBuilder);
        }

        @Override
        public AndroidSdk getAndroidSdk() {
            return new SdkTestSupport().getSdkWithPlatformDefault();
        }

        public void execute() {
            // Empty by design
        }
    }

    private class EmptyAndroidMojo extends AbstractAndroidMojo {
        @Inject
        protected EmptyAndroidMojo(ArtifactResolver artifactResolver, ArtifactHandler artHandler, MavenProjectHelper projectHelper, DependencyGraphBuilder dependencyGraphBuilder) {
            super(artifactResolver, artHandler, projectHelper, dependencyGraphBuilder);
        }

        public void execute() {
            // Empty by design
        }
    }
}
