/*
 * Copyright (C) 2014 The Android Open Source Project
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

/**
 * A {@link FullRevision} which distinguishes between x and x.0, x.0.0, x.y.0, etc.; it basically
 * keeps track of the precision of the revision string.
 * <p>
 * This is vital when referencing Gradle artifact numbers,
 * since versions x.y.0 and version x.y are not the same.
 */
public class PreciseRevision extends FullRevision {
    /**
     * The precision of the revision. This is the number of non-zero components in the revision.
     * <p>
     * For example, if the revision is "0000000", the precision is 3. If the revision is "1.2.0",
     * the precision is 2. If the revision is "1.0.0", the precision is 1. If the revision is "1.0",
     * the precision is 1. If the revision is "1", the precision is 1. If the revision is "0000000",
     * the precision is 4.
     */
    private final int mPrecision;

    /**
     * @param major     The major revision number.
     * @param minor     The minor revision number.
     * @param micro     The micro revision number.
     * @param preview   The preview number.
     * @param precision The precision of the revision. Must be one of the PRECISION_* constants.
     * @param separator The separator to use between the numbers.
     */
    PreciseRevision(int major, int minor, int micro, int preview, int precision,
                    String separator) {
        super(major, minor, micro, preview, separator);
        mPrecision = precision;
    }

    /**
     * Returns the version in a fixed format major.minor.micro
     * with an optional "rc preview#". For example, it would
     * return "18.0.0", "18.1.0" or "18.1.2 rc5".
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getMajor());

        if (mPrecision >= PRECISION_MINOR) {
            sb.append('.').append(getMinor());
            if (mPrecision >= PRECISION_MICRO) {
                sb.append('.').append(getMicro());
                if (mPrecision >= PRECISION_PREVIEW && isPreview()) {
                    sb.append(getSeparator()).append("rc").append(getPreview());
                }
            }
        }

        return sb.toString();
    }

    /**
     * @return The short string representation of this {@link PreciseRevision}.
     */
    @Override
    public String toShortString() {
        return toString();
    }

    /**
     * @param includePreview If true the output will contain 4 fields
     *                       to include the preview number (even if 0.) If false the output
     *                       will contain only 3 fields (major, minor and micro.)
     * @return An array of 3 or 4 integers.
     */
    @Override
    public int[] toIntArray(boolean includePreview) {
        int[] result;
        if (mPrecision >= PRECISION_PREVIEW && isPreview()) {
            if (includePreview) {
                result = new int[mPrecision];
                result[3] = getPreview();
            } else {
                result = new int[mPrecision - 1];
            }
        } else {
            result = new int[mPrecision];
        }
        result[0] = getMajor();
        if (mPrecision >= PRECISION_MINOR) {
            result[1] = getMinor();
            if (mPrecision >= PRECISION_MICRO) {
                result[2] = getMicro();
            }
        }

        return result;
    }

    /**
     * @return The hash code for this {@link PreciseRevision}.
     */
    @Override
    public int hashCode() {
        return 31 * super.hashCode() + mPrecision;
    }

    /**
     * @param rhs The right-hand side {@link FullRevision} to compare with.
     * @return {@code true} if {@code this} and {@code rhs} are equal.
     */
    @Override
    public boolean equals(Object rhs) {
        boolean equals = super.equals(rhs);
        if (equals) {
            if (!(rhs instanceof PreciseRevision other)) {
                return false;
            }
            return mPrecision == other.mPrecision;
        }
        return false;
    }
}