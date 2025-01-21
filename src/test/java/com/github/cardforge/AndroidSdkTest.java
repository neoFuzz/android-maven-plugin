/*
 * Copyright (C) 2009 Jayway AB
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

import com.github.cardforge.maven.plugins.android.AndroidSdk;
import com.github.cardforge.maven.plugins.android.InvalidSdkException;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;

/**
 * Exercises the {@link AndroidSdk} class.
 *
 * @author hugo.josefson@jayway.com
 * @author Manfred Moser - manfred@simpligility.com
 */
public class AndroidSdkTest {

    private SdkTestSupport sdkTestSupport;

    @Before
    public void setUp() {
        sdkTestSupport = new SdkTestSupport();
    }

    @Test
    public void givenToolAdbThenPathIsPlatformTools() {
        final String pathForTool = sdkTestSupport.getSdkWithPlatformDefault().getAdbPath();
        Assert.assertEquals(new File(sdkTestSupport.getenvAndroidHome() +
                "/platform-tools").getAbsolutePath(), new File(pathForTool).getParentFile().getAbsolutePath());
    }

    @Test
    public void givenToolAndroidThenPathIsCommon() {
        final String pathForTool = sdkTestSupport.getSdkWithPlatformDefault().getAndroidPath();
        Assert.assertEquals(new File(sdkTestSupport.getenvAndroidHome() +
                "/tools").getAbsolutePath(), new File(pathForTool).getParentFile().getAbsolutePath());
    }


    @Test(expected = InvalidSdkException.class)
    public void givenInvalidPlatformStringThenException() throws MojoExecutionException {
        final AndroidSdk sdk = new AndroidSdk(new File(sdkTestSupport.getenvAndroidHome()), "invalidplatform");
        System.out.println(sdk.getSdkPath());
    }

    @Test
    public void givenPlatformNullThenPlatformisSomethingValidLooking() throws IllegalAccessException, URISyntaxException {
        final File sdkPath = (File) ReflectionUtils.getValueIncludingSuperclasses(
                "sdkPath", sdkTestSupport.getSdkWithPlatformDefault());
        final File platform = sdkTestSupport.getSdkWithPlatformDefault().getPlatform();
        final String platformPath = platform.getAbsoluteFile().toURI().toString();
        final String regex = "/platforms/android-.*";
        //Strip off the sdkPath part  
        String matcher = platformPath.substring(sdkPath.toURI().toString().length() - 1);
        Assert.assertTrue(String.format("Platform [%s] does not match regex: [%s]", matcher, regex), matcher.matches(regex));
    }

    /**
     * Test all available platforms and api level versions. All have to be installed locally
     * for this test to pass including the obsolete ones.
     */
    @Test
    public void validPlatformsAndApiLevels19() {
        // Remember to add further platforms to .travis.yml if you add more platforms here, otherwise ci build fails
        final AndroidSdk sdk19 = new AndroidSdk(new File(sdkTestSupport.getenvAndroidHome()), "19");
        Assert.assertTrue(sdk19.getAaptPath() != null && !sdk19.getAaptPath().equals(""));
    }

    @Test
    public void validPlatformsAndApiLevels22() {
        // Remember to add further platforms to .travis.yml if you add more platforms here, otherwise ci build fails
        final AndroidSdk sdk22 = new AndroidSdk(new File(sdkTestSupport.getenvAndroidHome()), "22");
        Assert.assertTrue(sdk22.getAaptPath() != null && !sdk22.getAaptPath().equals(""));
    }

    @Test
    public void validPlatformsAndApiLevels25() {
        // Remember to add further platforms to .travis.yml if you add more platforms here, otherwise ci build fails
        final AndroidSdk sdk25 = new AndroidSdk(new File(sdkTestSupport.getenvAndroidHome()), "25");
        Assert.assertTrue(sdk25.getAaptPath() != null && !sdk25.getAaptPath().equals(""));
    }

    @Test
    public void validPlatformsAndApiLevels23() {
        // Remember to add further platforms to .travis.yml if you add more platforms here, otherwise ci build fails
        final AndroidSdk sdk23 = new AndroidSdk(new File(sdkTestSupport.getenvAndroidHome()), "23");
        Assert.assertTrue(sdk23.getAaptPath() != null && !sdk23.getAaptPath().equals(""));
    }

    @Test
    public void validPlatformsAndApiLevels28() {
        // Remember to add further platforms to .travis.yml if you add more platforms here, otherwise ci build fails
        final AndroidSdk sdk23 = new AndroidSdk(new File(sdkTestSupport.getenvAndroidHome()), "28");
        Assert.assertTrue(sdk23.getAaptPath() != null && !sdk23.getAaptPath().equals(""));
    }

    @Test
    public void validPlatformsAndApiLevels35() {
        // Remember to add further platforms to .travis.yml if you add more platforms here, otherwise ci build fails
        final AndroidSdk sdk35 = new AndroidSdk(new File(sdkTestSupport.getenvAndroidHome()), "35");
        Assert.assertTrue(sdk35.getAaptPath() != null && !sdk35.getAaptPath().equals(""));
    }

    @Test(expected = InvalidSdkException.class)
    public void invalidPlatformAndApiLevels() {
        final AndroidSdk invalid = new AndroidSdk(new File(sdkTestSupport.getenvAndroidHome()), "invalid");
        System.out.println(invalid.getAaptPath());
    }

    @Test(expected = NumberFormatException.class)
    public void invalidBuildTools() {
        final AndroidSdk invalid = new AndroidSdk(new File(sdkTestSupport.getenvAndroidHome()), "19", "invalid");
        invalid.getAaptPath();
    }

    @Test
    public void validPlatformsAndApiLevelsWithDiffBuildTools1() {
        // Remember to add further platforms to .travis.yml if you add more platforms here, otherwise ci build fails
        final AndroidSdk sdk = platformSetup("19", "28.0.3");
        Assert.assertTrue(sdk.getAaptPath() != null && !sdk.getAaptPath().equals(""));
    }

    @Test
    public void validPlatformsAndApiLevelsWithDiffBuildTools2() {
        // Remember to add further platforms to .travis.yml if you add more platforms here, otherwise ci build fails
        final AndroidSdk sdk = platformSetup("19", "28");
        Assert.assertTrue(sdk.getAaptPath() != null && !sdk.getAaptPath().equals(""));
    }

    @Test
    public void validPlatformsAndApiLevelsWithDiffBuildTools3() {
        // Remember to add further platforms to .travis.yml if you add more platforms here, otherwise ci build fails
        final AndroidSdk sdk = platformSetup("28", "30");
        Assert.assertTrue(sdk.getAaptPath() != null && !sdk.getAaptPath().equals(""));
    }

    @Test
    public void validPlatformsAndApiLevelsWithDiffBuildTools4() {
        // Remember to add further platforms to .travis.yml if you add more platforms here, otherwise ci build fails
        final AndroidSdk sdk = platformSetup("35", "35.0.1");
        Assert.assertTrue(sdk.getAaptPath() != null && !sdk.getAaptPath().isEmpty());
    }

    public AndroidSdk platformSetup(String api, String buildTools) {
        return new AndroidSdk(new File(sdkTestSupport.getenvAndroidHome()), api, buildTools);
    }

}
