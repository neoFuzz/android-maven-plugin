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

package com.android.sdklib.repository.descriptors;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.repository.FullRevision;
import com.android.sdklib.repository.License;
import com.android.sdklib.repository.MajorRevision;

import java.util.Objects;

/**
 * Implementation detail of {@link IPkgDescExtra} for extra packages.
 */
public final class PkgDescExtra extends PkgDesc implements IPkgDescExtra {

    private final String[] mOldPaths;
    private final String mNameDisplay;

    PkgDescExtra(@NonNull PkgType type,
                 @Nullable License license,
                 @Nullable String listDisplay,
                 @Nullable String descriptionShort,
                 @Nullable String descriptionUrl,
                 boolean isObsolete,
                 @Nullable FullRevision fullRevision,
                 @Nullable MajorRevision majorRevision,
                 @Nullable AndroidVersion androidVersion,
                 @Nullable String path,
                 @Nullable IdDisplay tag,
                 @Nullable IdDisplay vendor,
                 @Nullable FullRevision minToolsRev,
                 @Nullable FullRevision minPlatformToolsRev,
                 @NonNull String nameDisplay,
                 @Nullable final String[] oldPaths) {
        super(type,
                license,
                listDisplay,
                descriptionShort,
                descriptionUrl,
                isObsolete,
                fullRevision,
                majorRevision,
                androidVersion,
                path,
                tag,
                vendor,
                minToolsRev,
                minPlatformToolsRev,
                null,     //customIsUpdateFor
                null);    //customPath
        mNameDisplay = nameDisplay;
        mOldPaths = oldPaths != null ? oldPaths : new String[0];
    }

    /**
     * Helper method that converts the old_paths property string into the
     * old paths array.
     *
     * @param oldPathsProperty A possibly-null old_path property string.
     * @return A list of old paths split by their separator. Can be empty but not null.
     */
    @NonNull
    public static String[] convertOldPaths(@Nullable String oldPathsProperty) {
        if (oldPathsProperty == null || oldPathsProperty.isEmpty()) {
            return new String[0];
        }
        return oldPathsProperty.split(";");  //$NON-NLS-1$
    }

    /**
     * Helper to compute whether the extra path of both {@link IPkgDescExtra}s
     * are compatible with each other, which means they are either equal or are
     * matched between existing path and the potential old paths list.
     * <p/>
     * This also covers backward compatibility -- in earlier schemas the vendor id was
     * merged into the path string when reloading installed extras.
     *
     * @param lhs A non-null {@link IPkgDescExtra}.
     * @param rhs Another non-null {@link IPkgDescExtra}.
     * @return true if the paths are compatible.
     */
    public static boolean compatibleVendorAndPath(
            @NonNull IPkgDescExtra lhs,
            @NonNull IPkgDescExtra rhs) {
        String[] epOldPaths = rhs.getOldPaths();
        int lenEpOldPaths = epOldPaths.length;
        for (int indexEp = -1; indexEp < lenEpOldPaths; indexEp++) {
            if (sameVendorAndPath(
                    Objects.requireNonNull(lhs.getVendor()).getId(), lhs.getPath(),
                    Objects.requireNonNull(rhs.getVendor()).getId(), indexEp < 0 ?
                            rhs.getPath() : epOldPaths[indexEp])) {
                return true;
            }
        }

        String[] thisOldPaths = lhs.getOldPaths();
        int lenThisOldPaths = thisOldPaths.length;
        for (int indexThis = -1; indexThis < lenThisOldPaths; indexThis++) {
            if (sameVendorAndPath(
                    lhs.getVendor().getId(), indexThis < 0 ? lhs.getPath() : thisOldPaths[indexThis],
                    rhs.getVendor().getId(), rhs.getPath())) {
                return true;
            }
        }

        return false;
    }

    // ---- Helpers ----

    private static boolean sameVendorAndPath(
            @Nullable String thisVendor, @Nullable String thisPath,
            @Nullable String otherVendor, @Nullable String otherPath) {
        // To be backward compatible, we need to support the old vendor-path form
        // in either the current or the remote package.
        //
        // The vendor test below needs to account for an old installed package
        // (e.g. with an installation path of vendor-name) that has then been updated
        // in-place and thus when reloaded contains the vendor name in both the
        // path and the vendor attributes.
        if (otherPath != null && thisPath != null && thisVendor != null &&
                otherPath.equals(thisVendor + '-' + thisPath) &&
                    (otherVendor == null || otherVendor.isEmpty() || otherVendor.equals(thisVendor))) {
                return true;
            }

        if (thisPath != null && otherPath != null && otherVendor != null &&
                thisPath.equals(otherVendor + '-' + otherPath) &&
                    (thisVendor == null || thisVendor.isEmpty() || thisVendor.equals(otherVendor))) {
                return true;
            }


        return thisPath != null && thisPath.equals(otherPath) &&
                ((thisVendor == null && otherVendor == null) ||
                        (thisVendor != null && thisVendor.equals(otherVendor)));
    }

    @NonNull
    @Override
    public String[] getOldPaths() {
        return mOldPaths;
    }

    @NonNull
    @Override
    public String getNameDisplay() {
        return mNameDisplay;
    }

}

