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
package com.github.cardforge.maven.plugins.android;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.repository.Revision;
import com.android.repository.api.LocalPackage;
import com.android.sdklib.AndroidTargetHash;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.repository.AndroidSdkHandler;
import com.android.sdklib.repository.targets.AndroidTargetManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.function.Predicate;

/**
 * Represents an Android SDK.
 *
 * @author hugo.josefson@jayway.com
 * @author Manfred Moser - manfred@simpligility.com
 */
public class AndroidSdk {
    /**
     * Static string message for cannot find.
     */
    public static final String CANNOT_FIND_S = "Cannot find ";
    /**
     * the default API level for the SDK used as a fallback if none is supplied,
     * should ideally point to the latest available version
     */
    private static final String DEFAULT_ANDROID_API_LEVEL = "26";
    /**
     * property file in each platform folder with details about platform.
     */
    private static final String SOURCE_PROPERTIES_FILENAME = "source.properties";
    /**
     * property name for the sdk tools revision in sdk/tools/lib source.properties
     */
    private static final String SDK_TOOLS_REVISION_PROPERTY = "Pkg.Revision";
    /**
     * folder name for the sdk sub folder that contains the different platform versions.
     */
    private static final String PLATFORMS_FOLDER_NAME = "platforms";
    /**
     * folder name for the sdk sub folder that contains the different build tools versions.
     */
    private static final String BIN_FOLDER_NAME_IN_TOOLS = "bin";
    /**
     * Parameter message
     */
    private static final String PARAMETER_MESSAGE = "Please provide a proper Android SDK directory path as "
            + "configuration parameter <sdk><path>...</path></sdk> in the plugin <configuration/>. As an alternative,"
            + " you may add the parameter to commandline: -Dandroid.sdk.path=... or set environment variable "
            + AbstractAndroidMojo.ENV_ANDROID_HOME + ".";
    /**
     * Logger
     */
    private static final Logger log = LoggerFactory.getLogger(AndroidSdk.class);
    /**
     * The path to the Android SDK.
     */
    private final File sdkPath;
    /**
     * The Android target.
     */
    private final IAndroidTarget androidTarget;
    /**
     * The build tools version.
     */
    private final String buildToolsVersion;
    /**
     * Progress indicator.
     */
    private final ProgressIndicatorImpl progressIndicator;
    /**
     * Path to the platform tools.
     */
    private File platformToolsPath;
    /**
     * Path to the tools.
     */
    private File toolsPath;
    /**
     * The SDK manager.
     */
    private AndroidSdkHandler sdkManager;
    /**
     * The SDK tools major version.
     */
    private int sdkMajorVersion;

    /**
     * @param sdkPath  the SDK path to use
     * @param apiLevel the API level to use
     */
    public AndroidSdk(File sdkPath, String apiLevel) {
        this(sdkPath, apiLevel, null);
    }

    /**
     * @param sdkPath           SDK path containg the tools
     * @param apiLevel          API level to use
     * @param buildToolsVersion Build tool version to use
     */
    public AndroidSdk(File sdkPath, String apiLevel, @Nullable String buildToolsVersion) {
        this.sdkPath = sdkPath;
        this.buildToolsVersion = buildToolsVersion;
        this.progressIndicator = new ProgressIndicatorImpl();
        String path = "";

        if (sdkPath != null) {
            sdkManager = AndroidSdkHandler.getInstance(sdkPath);
            platformToolsPath = new File(sdkPath, SdkConstants.FD_PLATFORM_TOOLS);
            toolsPath = new File(sdkPath, SdkConstants.FD_TOOLS);

            if (sdkManager == null) {
                throw invalidSdkException(sdkPath, apiLevel);
            }
            path = sdkPath.getPath();
        }

        /*
         *  Note: The Android SDK Command-Line Tools package, located in cmdline-tools, replaces the SDK Tools package,
         *  located in tools. With the new package, you can select the version of the command line tools you want to
         *  install, and you can install multiple versions at a time. With the old package, you can only install the
         *  latest version of the tools. Thus, the new package lets you depend on specific versions of the command-line
         *  tools without having your code break when new versions are released. For information about the deprecated
         *  SDK Tools package, see the SDK Tools release notes. TODO: Android SDK Command-Line Tools
         */
        //loadSDKToolsMajorVersion(); // noinspection CommentedOutCode

        if (apiLevel == null) {
            apiLevel = DEFAULT_ANDROID_API_LEVEL;
        }
        log.info("API: {} | SDK Path: {} | Buildtools: {}", apiLevel, path, buildToolsVersion);

        androidTarget = findPlatformByApiLevel(apiLevel);
        if (androidTarget == null) {
            throw invalidSdkException(new File(path), apiLevel);
        }
    }

    /**
     * @param windowsExtension    Extension used by Windows
     * @param nonWindowsExtension Extension used by non-Windows OSes
     * @return the correct extension for the current platform.
     */
    private static String ext(String windowsExtension, String nonWindowsExtension) {
        if (SdkConstants.currentPlatform() == SdkConstants.PLATFORM_WINDOWS) {
            return windowsExtension;
        } else {
            return nonWindowsExtension;
        }
    }

    /**
     * @param sdkPath            the SDK path to use
     * @param platformOrApiLevel the API level or platform to use
     * @return the exception to throw
     */
    private InvalidSdkException invalidSdkException(@NonNull File sdkPath, String platformOrApiLevel) {
        throw new InvalidSdkException("Invalid SDK: Platform/API level " + platformOrApiLevel
                + " not available. This command should give you all you need:\n" + sdkPath.getAbsolutePath()
                + File.separator + "tools" + File.separator + "android update sdk --no-ui --obsolete --force");
    }

    /**
     * @param apiLevel the API level to use
     * @return the platform for the given API level
     */
    @Nullable
    private IAndroidTarget findPlatformByApiLevel(String apiLevel) {
        // try to find by api level first
        AndroidVersion version;
        try {
            version = new AndroidVersion(apiLevel);
            String hashString = AndroidTargetHash.getPlatformHashString(version);
            IAndroidTarget target = sdkManager.getAndroidTargetManager(progressIndicator)
                    .getTargetFromHashString(hashString, progressIndicator);

            // SdkManager may return a non-null IAndroidTarget that references nothing.
            // I suspect it points to an SDK that has been removed.
            if (target != null && target.getLocation() != null) {
                return target;
            }
        } catch (AndroidVersion.AndroidVersionException e) {
            throw new InvalidSdkException("Error AndroidVersion: " + e.getMessage());
        }

        // fallback to searching for platform on standard Android platforms (isPlatform() is true)
        for (IAndroidTarget t : sdkManager.getAndroidTargetManager(null).getTargets(null)) {
            if (t.isPlatform() && apiLevel.equals(t.getVersionName())) {
                return t;
            }
        }
        return null;
    }

    /**
     * @param path Path to check is a directory
     */
    private void assertPathIsDirectory(final File path) {
        if (path == null) {
            throw new InvalidSdkException(PARAMETER_MESSAGE);
        }
        if (!path.isDirectory()) {
            throw new InvalidSdkException("Path \"" + path + "\" is not a directory. " + PARAMETER_MESSAGE);
        }
    }

    /**
     * Get the aapt tool path.
     *
     * @return the path to the aapt tool
     */
    public String getAaptPath() {
        return getPathForBuildTool(BuildToolInfo.PathId.AAPT);
    }

    /**
     * Get the aild tool path
     *
     * @return the path to the aidl tool
     */
    public String getAidlPath() {
        return getPathForBuildTool(BuildToolInfo.PathId.AIDL);
    }

    /**
     * Get the path for dx.jar
     *
     * @return the path to the dx.jar
     */
    public String getDxJarPath() {
        return getPathForBuildTool(BuildToolInfo.PathId.DX_JAR);
    }

    /**
     * @return the path to the dx.jar
     */
    public String getD8JarPath() {
        final File pathToDexJar = new File(getPathForBuildTool(BuildToolInfo.PathId.DX_JAR));
        final File pathToD8Jar = new File(pathToDexJar.getParent(), "d8.jar");
        return pathToD8Jar.getAbsolutePath();
    }

    /**
     * Get the path for proguard.jar
     *
     * @return the path to the proguard.jar
     */
    public String getProguardJarPath() {
        File directory = new File(getToolsPath(), "proguard" + File.separator + "lib" + File.separator);
        File proguardJar = new File(directory, "proguard.jar");
        if (proguardJar.exists()) {
            return proguardJar.getAbsolutePath();
        }
        throw new InvalidSdkException(CANNOT_FIND_S + proguardJar);
    }

    /**
     * Get the path for shrinkedAndroid.jar
     *
     * @return the path to the shrinkedAndroid.jar
     */
    public String getShrinkedAndroidJarPath() {
        File shrinkedAndroidJar = new File(getBuildToolsLibDirectoryPath(), "shrinkedAndroid.jar");
        if (shrinkedAndroidJar.exists()) {
            return shrinkedAndroidJar.getAbsolutePath();
        }
        throw new InvalidSdkException(CANNOT_FIND_S + shrinkedAndroidJar);
    }

    /**
     * Get the path for build-tools lib directory
     *
     * @return the path to the build-tools lib directory
     */
    public String getBuildToolsLibDirectoryPath() {
        File buildToolsLib = new File(getBuildToolInfo().getLocation(), "lib");
        if (buildToolsLib.exists()) {
            return buildToolsLib.getAbsolutePath();
        }
        throw new InvalidSdkException(CANNOT_FIND_S + buildToolsLib);
    }

    /**
     * Get the path for {@code mainDexClasses.rules}
     *
     * @return the path to the {@code mainDexClasses.rules}
     */
    public String getMainDexClassesRulesPath() {
        File mainDexClassesRules = new File(getBuildToolInfo().getLocation(),
                "mainDexClasses.rules");
        if (mainDexClassesRules.exists()) {
            return mainDexClassesRules.getAbsolutePath();
        }
        throw new InvalidSdkException(CANNOT_FIND_S + mainDexClassesRules);
    }

    /**
     * @param version the version to check against
     * @param feature the feature that requires the version
     * @throws InvalidSdkException   if the version is not met
     * @throws NumberFormatException if the version is not a number
     */
    public void assertThatBuildToolsVersionIsAtLeast(String version, String feature)
            throws InvalidSdkException, NumberFormatException {
        if (getBuildToolInfo().getRevision().
                compareTo(Revision.parseRevision(version)) < 0) {
            throw new InvalidSdkException("Version of build tools must be at least "
                    + version + " for " + feature + " to work");
        }
    }

    /**
     * Get the android debug tool path (adb).
     *
     * @return the path to the adb tool
     */
    public String getAdbPath() {
        return getPathForPlatformTool(SdkConstants.FN_ADB);
    }

    /**
     * Get the android zipalign path.
     *
     * @return the path to the zipalign tool
     */
    public String getZipalignPath() {
        return getPathForBuildTool(BuildToolInfo.PathId.ZIP_ALIGN);
    }

    /**
     * Get the android lint path.
     *
     * @return the path to the lint tool
     */
    public String getLintPath() {
        return getPathForTool(BIN_FOLDER_NAME_IN_TOOLS + "/" + "lint" + ext(".bat", ""));
    }

    /**
     * Get the android monkey runner path.
     *
     * @return the path to the monkeyrunner tool
     */
    public String getMonkeyRunnerPath() {
        return getPathForTool(BIN_FOLDER_NAME_IN_TOOLS + "/" + "monkeyrunner" + ext(".bat", ""));
    }

    /**
     * Get the apkbuilder path.
     *
     * @return the path to the apkbuilder tool
     */
    public String getApkBuilderPath() {
        return getPathForTool("apkbuilder" + ext(".bat", ""));
    }

    /**
     * Get the android tool path.
     *
     * @return the path to the android tool
     */
    public String getAndroidPath() {
        String cmd = "android";
        String ext = SdkConstants.currentPlatform() == 2 ? ".bat" : "";

        return getPathForTool(cmd + ext);
    }

    /**
     * Get the path to the tools' directory.
     *
     * @return the path to the tools directory
     */
    public File getToolsPath() {
        return toolsPath;
    }

    /**
     * @param pathId the path to retrieve
     * @return the path to the given tool, based on this SDK.
     */
    private String getPathForBuildTool(BuildToolInfo.PathId pathId) {
        return getBuildToolInfo().getPath(pathId);
    }

    /**
     * @return the path to the build tools directory
     */
    @NonNull
    private BuildToolInfo getBuildToolInfo() {
        //First we use the build tools specified in the pom file
        if (buildToolsVersion != null && !buildToolsVersion.isEmpty()) {
            BuildToolInfo buildToolInfo = sdkManager.getBuildToolInfo(Revision.parseRevision(buildToolsVersion),
                    progressIndicator);
            if (buildToolInfo != null) {
                return buildToolInfo;
            } else {
                //If the build tools specified by the user is not installed, we try to find the latest
                //installed revision of the build tools
                BuildToolInfo latestBuildToolInfo = BuildToolInfo.fromLocalPackage(
                        getLatestBuildToolForMajorVersion(Integer.parseInt(buildToolsVersion)));
                if (latestBuildToolInfo != null) {
                    return latestBuildToolInfo;
                }
            }
            //Since we cannot find the build tool specified by the user we make it fail
            // instead of using the latest build tool version
            throw new InvalidSdkException("Invalid SDK: Build-tools " + buildToolsVersion + " not found."
                    + " Check your Android SDK to install the build tools " + buildToolsVersion);
        }

        if (androidTarget != null) {
            BuildToolInfo buildToolInfo = androidTarget.getBuildToolInfo();
            if (buildToolInfo != null) {
                return buildToolInfo;
            }
        }
        // if no valid target is defined, or it has no build tools installed, try to use the latest
        BuildToolInfo latestBuildToolInfo = sdkManager.getLatestBuildTool(progressIndicator, true);
        if (latestBuildToolInfo == null) {
            throw new InvalidSdkException("Invalid SDK: Build-tools not found. Check the content of '"
                    + sdkPath.getAbsolutePath() + File.separator + "build-tools', or run '"
                    + sdkPath.getAbsolutePath() + File.separator + "tools" + File.separator
                    + "android sdk' to install them");
        }
        return latestBuildToolInfo;
    }

    /**
     * @param tool the name of the tool to retrieve the path for.
     * @return the path to the given tool, based on this SDK.
     */
    @NonNull
    private String getPathForPlatformTool(String tool) {
        return new File(platformToolsPath, tool).getAbsolutePath();
    }

    /**
     * @param tool the name of the tool to retrieve the path for.
     * @return the path to the given tool, based on this SDK.
     */
    @NonNull
    private String getPathForTool(String tool) {
        return new File(toolsPath, tool).getAbsolutePath();
    }

    /**
     * @param majorVersion the major version of the build tools to retrieve
     * @return the latest build tool for the given major version
     */
    public LocalPackage getLatestBuildToolForMajorVersion(int majorVersion) {
        // Define the prefix for build tools
        String prefix = "build-tools";

        // Define the predicate to match the major version
        Predicate<Revision> majorVersionFilter = revision -> revision.getMajor() == majorVersion;

        // Fetch the latest local package that matches the prefix and major version
        LocalPackage latestPackage = sdkManager.getLatestLocalPackageForPrefix(
                prefix, majorVersionFilter, false, progressIndicator);

        if (latestPackage == null) {
            throw new IllegalArgumentException("No build-tools found for major version: " + majorVersion);
        }

        return latestPackage;
    }

    /**
     * Returns the complete path for <code>framework.aidl</code>, based on this SDK.
     *
     * @return the complete path as a <code>String</code>, including the filename.
     */
    public String getPathForFrameworkAidl() {
        return androidTarget.getPath(IAndroidTarget.ANDROID_AIDL);
    }

    /**
     * Resolves the android.jar from this SDK.
     *
     * @return a <code>File</code> pointing to the android.jar file.
     * @throws MojoExecutionException if the file can not be resolved.
     */
    public File getAndroidJar() throws MojoExecutionException {
        final String androidJarPath = androidTarget.getPath(IAndroidTarget.ANDROID_JAR);
        if (androidJarPath == null) {
            throw new MojoExecutionException("No AndroidJar found for " + androidTarget.getLocation());
        }
        return new File(androidJarPath);
    }

    /**
     * Resolves the path for this SDK.
     *
     * @return a <code>File</code> pointing to the SDk Directory.
     * @throws MojoExecutionException if the file can not be resolved.
     */
    public File getSdkPath() throws MojoExecutionException {
        if (sdkPath.exists()) {
            return sdkPath;
        }
        throw new MojoExecutionException("Can't find the SDK directory : " + sdkPath.getAbsolutePath());
    }

    /**
     * This method returns the previously specified version. However, if none have been specified it returns the
     * "latest" version.
     *
     * @return the platform version as a <code>String</code>.
     */
    public File getPlatform() {
        assertPathIsDirectory(sdkPath);

        final File platformsDirectory = new File(sdkPath, PLATFORMS_FOLDER_NAME);
        assertPathIsDirectory(platformsDirectory);

        final File platformDirectory;
        if (androidTarget == null) {
            IAndroidTarget latestTarget = null;
            AndroidTargetManager targetManager = sdkManager.getAndroidTargetManager(progressIndicator);
            for (IAndroidTarget target : targetManager.getTargets(progressIndicator)) {
                if (target.isPlatform() && (latestTarget == null
                        || target.getVersion().getApiLevel() > latestTarget.getVersion().getApiLevel())) {
                    latestTarget = target;
                }

            }
            platformDirectory = new File(latestTarget.getLocation());
        } else {
            platformDirectory = new File(androidTarget.getLocation());
        }
        assertPathIsDirectory(platformDirectory);
        return platformDirectory;
    }

    /**
     * Loads the SDK Tools version
     */
    private void loadSDKToolsMajorVersion() {
        File propFile = new File(sdkPath, "tools/" + SOURCE_PROPERTIES_FILENAME);
        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream(propFile)) {
            properties.load(fis);
        } catch (IOException ignored) {
            throw new InvalidSdkException("Error reading " + propFile.getAbsoluteFile());
        }

        if (properties.containsKey(SDK_TOOLS_REVISION_PROPERTY)) {
            try {
                String versionString = properties.getProperty(SDK_TOOLS_REVISION_PROPERTY);
                String majorVersion;
                if (versionString.matches(".*[\\.| ].*")) {
                    String[] versions = versionString.split("[\\.| ]");
                    majorVersion = versions[0];
                } else {
                    majorVersion = versionString;
                }
                sdkMajorVersion = Integer.parseInt(majorVersion);
            } catch (NumberFormatException e) {
                throw new InvalidSdkException("Error - The property '" + SDK_TOOLS_REVISION_PROPERTY
                        + "' in the SDK source.properties file  number is not an Integer: "
                        + properties.getProperty(SDK_TOOLS_REVISION_PROPERTY));
            }
        }
    }

    /**
     * Returns the version of the SDK Tools.
     *
     * @return the version of the SDK Tools as an <code>int</code>.
     */
    public int getSdkMajorVersion() {
        return sdkMajorVersion;
    }
}
