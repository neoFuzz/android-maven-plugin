/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.sdklib;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

import java.io.File;


/**
 * A version of Android that applications can target when building.
 */
public interface IAndroidTarget extends Comparable<IAndroidTarget> {
    /**
     * OS Path to the "android.jar" file.
     */
    int ANDROID_JAR = 1;
    /**
     * OS Path to the "framework.aidl" file.
     */
    int ANDROID_AIDL = 2;

    /**
     * Returns the target location.
     *
     * @return the target location.
     */
    String getLocation();

    /**
     * Returns the name of the vendor of the target.
     *
     * @return the name of the vendor of the target.
     */
    String getVendor();

    /**
     * Returns the name of the target.
     *
     * @return the name of the target.
     */
    String getName();

    /**
     * Returns the description of the target.
     *
     * @return the description of the target.
     */
    String getDescription();

    /**
     * Returns the version of the target. This is guaranteed to be non-null.
     *
     * @return the version of the target.
     */
    @NonNull
    AndroidVersion getVersion();

    /**
     * Returns the platform version as a readable string.
     *
     * @return the platform version or null if this is not a platform.
     */
    String getVersionName();

    /**
     * Returns true if the target is a standard Android platform.
     *
     * @return True if the target is a standard Android platform.
     */
    boolean isPlatform();

    /**
     * Returns the path of a platform component.
     *
     * @param pathId the id representing the path to return.
     *               Any of the constants defined in the {@link IAndroidTarget} interface can be used.
     * @return the path as a string or null if the path is not available.
     */
    String getPath(int pathId);

    /**
     * Returns a BuildToolInfo for backward compatibility. If an older SDK is used this will return
     * paths located in the platform-tools, otherwise it'll return paths located in the latest
     * build-tools.
     *
     * @return a BuildToolInfo or null if none are available.
     */
    BuildToolInfo getBuildToolInfo();

    /**
     * Returns the available skin folders for this target.
     * <p>
     * To get the skin names, use {@link File#getName()}. <br/>
     * Skins come either from:
     * <ul>
     * <li>a platform ({@code sdk/platforms/N/skins/name})</li>
     * <li>an add-on ({@code sdk/addons/name/skins/name})</li>
     * <li>a tagged system-image ({@code sdk/system-images/platform-N/tag/abi/skins/name}.)</li>
     * </ul>
     * The array can be empty but not null.
     *
     * @return an array of skin folders.
     */
    @NonNull
    @SuppressWarnings("unused") // It is used somehow since it broke the Android Maven Plugin
    File[] getSkins();

    /**
     * Returns the default skin folder for this target.
     * <p>
     * To get the skin name, use {@link File#getName()}.
     *
     * @return the default skin or <code>null</code> if there is none.
     */
    @Nullable
    @SuppressWarnings("unused") // It is used somehow since it broke the Android Maven Plugin
    File getDefaultSkin();
}