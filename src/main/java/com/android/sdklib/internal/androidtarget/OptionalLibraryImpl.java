/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.sdklib.internal.androidtarget;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.IAndroidTarget;

import java.io.File;

/**
 * Internal implementation of OptionalLibrary
 */
public class OptionalLibraryImpl implements IAndroidTarget.OptionalLibrary {

    /**
     * The name of the library, as it should be displayed to the user
     */
    @NonNull
    private final String mLibraryName;
    /**
     * The location of the jar file
     */
    @NonNull
    private final File mJarFile;
    /**
     * A description of the library
     */
    @NonNull
    private final String mDescription;
    /**
     * Whether the library is required to be present in the manifest file
     */
    private final boolean mRequireManifestEntry;

    /**
     * @param libraryName          the name of the library, as it should be displayed to the user
     * @param jarFile              the location of the jar file
     * @param description          a description of the library
     * @param requireManifestEntry whether the library is required to be present in the manifest file
     */
    public OptionalLibraryImpl(
            @NonNull String libraryName,
            @NonNull File jarFile,
            @NonNull String description,
            boolean requireManifestEntry) {
        mLibraryName = libraryName;
        mJarFile = jarFile;
        mDescription = description;
        mRequireManifestEntry = requireManifestEntry;
    }

    @Override
    @NonNull
    public String getName() {
        return mLibraryName;
    }

    @Override
    @NonNull
    public File getJar() {
        return mJarFile;
    }

    @Override
    @NonNull
    public String getDescription() {
        return mDescription;
    }

    @Override
    public boolean isManifestEntryRequired() {
        return mRequireManifestEntry;
    }

    @Nullable
    @Override
    public String getLocalJarPath() {
        return "";
    }
}
