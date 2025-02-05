/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdklib.repository;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * License text, with an optional license XML reference.
 */
public class License {
    /**
     * Directory where licenses are stored.
     */
    private static final String LICENSE_DIR = "licenses";
    /**
     * License text
     */
    private final String mLicense;
    /**
     * License XML reference, or null.
     */
    private final String mLicenseRef;
    /**
     * Hash of the license text. Never null.
     */
    private final String mLicenseHash;

    /**
     * @param license    The license text.
     * @param licenseRef The license XML reference, or null.
     */
    public License(@NonNull String license, @Nullable String licenseRef) {
        mLicense = license;
        mLicenseRef = licenseRef;
        mLicenseHash = Hashing.sha1().hashBytes(mLicense.getBytes()).toString();
    }

    /**
     * Returns the license text. Never null.
     *
     * @return the license text
     */
    @NonNull
    public String getLicense() {
        return mLicense;
    }

    /**
     * Returns the hash of the license text. Never null.
     *
     * @return the license hash
     */
    @NonNull
    public String getLicenseHash() {
        return mLicenseHash;
    }

    /**
     * Returns the license XML reference.
     * Could be null, e.g. in tests or synthetic packages
     * recreated from local source.properties.
     *
     * @return the license XML reference, or null
     */
    @Nullable
    public String getLicenseRef() {
        return mLicenseRef;
    }

    /**
     * Returns a string representation of the license, useful for debugging.
     * This is not designed to be shown to the user.
     *
     * @return a string representation of the license
     */
    @Override
    public String toString() {
        return "<License ref:" + mLicenseRef + ", text:" + mLicense + ">";
    }

    /**
     * @return the hash code for this license
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((mLicense == null) ? 0 : mLicense.hashCode());
        result = prime * result
                + ((mLicenseRef == null) ? 0 : mLicenseRef.hashCode());
        return result;
    }

    /**
     * @param obj The object to compare with this license
     * @return true if the given object is equal to this license, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof License other)) {
            return false;
        }
        if (mLicense == null) {
            if (other.mLicense != null) {
                return false;
            }
        } else if (!mLicense.equals(other.mLicense)) {
            return false;
        }
        if (mLicenseRef == null) {
            return other.mLicenseRef == null;
        } else return mLicenseRef.equals(other.mLicenseRef);
    }

    /**
     * Checks whether this license has previously been accepted.
     *
     * @param sdkRoot The root directory of the Android SDK
     * @return true if this license has already been accepted
     */
    public boolean checkAccepted(@Nullable File sdkRoot) {
        if (sdkRoot == null) {
            return false;
        }
        File licenseDir = new File(sdkRoot, LICENSE_DIR);
        File licenseFile = new File(licenseDir, mLicenseRef == null ? mLicenseHash : mLicenseRef);
        if (!licenseFile.exists()) {
            return false;
        }
        try {
            String hash = Files.readFirstLine(licenseFile, StandardCharsets.UTF_8);
            return hash != null && hash.equals(mLicenseHash);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Marks this license as accepted.
     *
     * @param sdkRoot The root directory of the Android SDK
     * @return true if the acceptance was persisted successfully.
     */
    public boolean setAccepted(@Nullable File sdkRoot) {
        if (sdkRoot == null) {
            return false;
        }
        if (checkAccepted(sdkRoot)) {
            return true;
        }
        File licenseDir = new File(sdkRoot, LICENSE_DIR);
        if (licenseDir.exists() && !licenseDir.isDirectory()) {
            return false;
        }
        if (!licenseDir.exists()) {
            licenseDir.mkdir();
        }
        File licenseFile = new File(licenseDir, mLicenseRef == null ? mLicenseHash : mLicenseRef);
        try {
            Files.write(mLicenseHash, licenseFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}