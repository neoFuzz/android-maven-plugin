/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.builder.internal;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISystemImage;
import com.android.sdklib.repository.descriptors.IdDisplay;
import com.android.utils.SparseArray;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fake IAndroidTarget used for SDK prebuilt in the Android source tree.
 */
public class FakeAndroidTarget implements IAndroidTarget {
    /**
     * Static string with 'android' for compatibility with {@link ISystemImage#getTag()}
     */
    public static final String ANDROID = "android";
    /**
     * Static string with 'android-' for compatibility with {@link ISystemImage#getTag()}
     */
    public static final String ANDROIDH = "android-";
    /**
     * String with the SDK location
     */
    private final String mSdkLocation;
    /**
     * Map with the paths to the platform components
     */
    private final SparseArray<String> mPaths = new SparseArray<>();
    /**
     * List with the boot classpath
     */
    private final List<String> mBootClasspath = Lists.newArrayListWithExpectedSize(2);
    /**
     * API level of the target
     */
    private final int mApiLevel;

    /**
     * @param sdkLocation the SDK location
     * @param target      the target name, e.g. "current", "android-18", "unstubbed"
     */
    public FakeAndroidTarget(String sdkLocation, String target) {
        mSdkLocation = sdkLocation;
        mApiLevel = getApiLevel(target);

        if ("unstubbed".equals(target)) {
            mBootClasspath.add(mSdkLocation +
                    "/out/target/common/obj/JAVA_LIBRARIES/framework_intermediates/classes.jar");
            mBootClasspath.add(mSdkLocation +
                    "/out/target/common/obj/JAVA_LIBRARIES/core_intermediates/classes.jar");

            // pre-build the path to the platform components
            mPaths.put(ANDROID_JAR, mSdkLocation + "/prebuilts/sdk/current/" +
                    SdkConstants.FN_FRAMEWORK_LIBRARY);
            mPaths.put(ANDROID_AIDL, mSdkLocation + "/prebuilts/sdk/renderscript/" +
                    SdkConstants.FN_FRAMEWORK_AIDL);
        } else {
            String apiPrebuilts;

            if ("current".equals(target)) {
                apiPrebuilts = mSdkLocation + "/prebuilts/sdk/current/";
            } else {
                apiPrebuilts = mSdkLocation + "/prebuilts/sdk/" + mApiLevel + "/";
            }

            // pre-build the path to the platform components
            mBootClasspath.add(apiPrebuilts + SdkConstants.FN_FRAMEWORK_LIBRARY);
            mPaths.put(ANDROID_JAR, apiPrebuilts + SdkConstants.FN_FRAMEWORK_LIBRARY);
            mPaths.put(ANDROID_AIDL, apiPrebuilts + SdkConstants.FN_FRAMEWORK_AIDL);
        }
    }

    /**
     * @param target the target name, e.g. "current", "android-18", "unstubbed"
     * @return the API level of the target
     */
    private int getApiLevel(@NonNull String target) {
        if (target.startsWith(ANDROIDH)) {
            return Integer.parseInt(target.substring(ANDROIDH.length()));
        }

        // We don't actually know the API level at this point since the mode is "current"
        // or "unstubbed". This API is only called to check if annotations.jar needs to be
        // added to the classpath, so by putting a large value we make sure annotations.jar
        // isn't used.
        return 99;
    }

    /**
     * @param pathId the id representing the path to return.
     *               Any of the constants defined in the {@link IAndroidTarget} interface can be used.
     * @return The absolute path for that tool, with a / separator if it's a folder.
     * Null if the path-id is unknown.
     */
    @Override
    public String getPath(int pathId) {
        return mPaths.get(pathId);
    }

    /**
     * @param pathId the id representing the path to return.
     *               Any of the constants defined in the {@link IAndroidTarget} interface can be used.
     * @return The absolute path for that tool, with a / separator if it's a folder.
     * Null if the path-id is unknown.
     */
    @Override
    public File getFile(int pathId) {
        return new File(getPath(pathId));
    }

    /**
     * @return The {@link BuildToolInfo} for this target, or null if there is none.
     */
    @Override
    public BuildToolInfo getBuildToolInfo() {
        // this is not used internally since we properly query for the right Build Tools from
        // the SdkManager.
        return null;
    }

    /**
     * @return The list of boot classpath entries for this target
     */
    @Override
    @NonNull
    public List<String> getBootClasspath() {
        return mBootClasspath;
    }

    /**
     * @return The location of the SDK.
     */
    @Override
    public String getLocation() {
        return mSdkLocation;
    }

    /**
     * @return The vendor of the target, which is always "android" for platform targets.
     */
    @Override
    public String getVendor() {
        return ANDROID;
    }

    /**
     * @return The name of the target, which is always "android" for platform targets.
     */
    @Override
    public String getName() {
        return ANDROID;
    }

    /**
     * @return The full name of the target, which is always "android" for platform targets.
     */
    @Override
    public String getFullName() {
        return ANDROID;
    }

    /**
     * @return The classpath name of the target, which is always "android" for platform targets.
     */
    @Override
    public String getClasspathName() {
        return ANDROID;
    }

    /**
     * @return The short classpath name of the target, which is always "android" for platform targets.
     */
    @Override
    public String getShortClasspathName() {
        return ANDROID;
    }

    /**
     * @return The description of the target, which is always "android" for platform targets.
     */
    @Override
    public String getDescription() {
        return ANDROID;
    }

    /**
     * @return The version of the target
     */
    @NonNull
    @Override
    public AndroidVersion getVersion() {
        return new AndroidVersion(mApiLevel, null);
    }

    /**
     * @return The version name of the target
     */
    @Override
    public String getVersionName() {
        return "Android API level " + mApiLevel;
    }

    /**
     * @return The revision of the target
     */
    @Override
    public int getRevision() {
        return 1;
    }

    /**
     * @return True if the target is a platform, false otherwise
     */
    @Override
    public boolean isPlatform() {
        return true;
    }

    /**
     * @return The parent target, or null if there is none
     */
    @Override
    public IAndroidTarget getParent() {
        return null;
    }

    /**
     * @return false, no rendering library is available for platform targets
     */
    @Override
    public boolean hasRenderingLibrary() {
        return false;
    }

    /**
     * @return The list of available skins for this target
     */
    @NonNull
    @Override
    public File[] getSkins() {
        return new File[0];
    }

    /**
     * @return null, there is none
     */
    @Override
    public File getDefaultSkin() {
        return null;
    }

    /**
     * @return The list of available additional libraries for this target
     */
    @NonNull
    @Override
    public List<OptionalLibrary> getAdditionalLibraries() {
        return ImmutableList.of();
    }

    /**
     * @return The list of available optional libraries for this target
     */
    @NonNull
    @Override
    public List<OptionalLibrary> getOptionalLibraries() {
        return ImmutableList.of();
    }

    /**
     * @return The list of available platform libraries for this target
     */
    @Override
    public String[] getPlatformLibraries() {
        return new String[0];
    }

    /**
     * @param name the name of the property to return
     * @return null, there is none
     */
    @Override
    public String getProperty(String name) {
        return null;
    }

    /**
     * @param name         the name of the property to return
     * @param defaultValue the default value to return.
     * @return null, there is none
     */
    @Override
    public Integer getProperty(String name, Integer defaultValue) {
        return null;
    }

    /**
     * @param name         the name of the property to return
     * @param defaultValue the default value to return.
     * @return false, there is none
     */
    @Override
    public Boolean getProperty(String name, Boolean defaultValue) {
        return false;
    }

    /**
     * @return The map of all properties for this target
     */
    @Override
    public Map<String, String> getProperties() {
        return new HashMap<>();
    }

    /**
     * @return The USB vendor ID, which is always 0 for platform targets
     */
    @Override
    public int getUsbVendorId() {
        return 0;
    }

    /**
     * @return The list of system images for this target
     */
    @Override
    public ISystemImage[] getSystemImages() {
        return new ISystemImage[0];
    }

    /**
     * @param tag     A tag id-display.
     * @param abiType An ABI type string.
     * @return null, there is none
     */
    @Override
    public ISystemImage getSystemImage(@NonNull IdDisplay tag, @NonNull String abiType) {
        return null;
    }

    /**
     * @param target the IAndroidTarget to test.
     * @return false, platform targets can't run on other targets
     */
    @Override
    public boolean canRunOn(IAndroidTarget target) {
        return false;
    }

    /**
     * @return The hash string for this target
     */
    @Override
    public String hashString() {
        return ANDROIDH + mApiLevel;
    }

    /**
     * @param iAndroidTarget the object to be compared.
     * @return the result of the comparison
     */
    @Override
    public int compareTo(@NonNull IAndroidTarget iAndroidTarget) {
        FakeAndroidTarget that = (FakeAndroidTarget) iAndroidTarget;
        return mSdkLocation.compareTo(that.mSdkLocation);
    }
}
