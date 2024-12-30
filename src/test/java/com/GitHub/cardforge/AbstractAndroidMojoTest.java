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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.fest.reflect.core.Reflection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author hugo.josefson@jayway.com
 */
public class AbstractAndroidMojoTest {
    public static final String BASE_PATH = "maven/plugins/android/";
    protected AbstractAndroidMojo androidMojo;

    @Before
    public void setUp() {
        androidMojo = new DefaultTestAndroidMojo();
    }

    @Test
    public void givenNoPathThenUseAndroidHomePath() throws MojoExecutionException {
        SdkTestSupport testSupport = new SdkTestSupport();
        androidMojo = new EmptyAndroidMojo();
        Reflection.field("sdkPath").ofType(File.class).in(androidMojo).set(null);
        Reflection.field("sdkPlatform").ofType(String.class).in(androidMojo).set("19");
        AndroidSdk sdk = androidMojo.getAndroidSdk();
        File path = Reflection.field("sdkPath").ofType(File.class).in(sdk).get();
        Assert.assertEquals(new File(testSupport.getEnv_ANDROID_HOME()).getAbsolutePath(), path.getAbsolutePath());
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
            throws URISyntaxException, MojoExecutionException {
        final URL url = this.getClass().getResource( BASE_PATH + "AndroidManifest.xml");
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
    public void givenApidemosApkThenPackageIsFound() throws MojoExecutionException, URISyntaxException {
        final URL resource = this.getClass().getResource(BASE_PATH + "apidemos-0.1.0-SNAPSHOT.apk");
        final String foundPackage = androidMojo.extractPackageNameFromApk(new File(new URI(resource.toString())));
        Assert.assertEquals("com.example.android.apis", foundPackage);
    }

    @Test
    public void givenApidemosPlatformtestsApkThenPackageIsFound() throws MojoExecutionException, URISyntaxException {
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

    private class DefaultTestAndroidMojo extends AbstractAndroidMojo {

        @Override
        public AndroidSdk getAndroidSdk() {
            return new SdkTestSupport().getSdk_with_platform_default();
        }

        public void execute() {

        }
    }

    private class EmptyAndroidMojo extends AbstractAndroidMojo {
        public void execute() {
        }
    }
}
