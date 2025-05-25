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

package com.android.sdklib;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

import java.io.Serial;
import java.util.regex.Pattern;

/**
 * Represents the version of a target or device.
 * <p>
 * A version is defined by an API level and an optional code name.
 * <ul><li>Release versions of the Android platform are identified by their API level (integer),
 * (technically the code name for release version is "REL" but this class will return
 * <code>null</code> instead.)</li>
 * <li>Preview versions of the platform are identified by a code name. Their API level
 * is usually set to the value of the previous platform.</li>
 * </ul>
 * While this class contains both values, its goal is to abstract them, so that code comparing 2+
 * versions doesn't have to deal with the logic of handle both values.
 * <p>
 * There are some cases where ones may want to access the values directly. This can be done
 * with {@link #getApiLevel()}.
 * </p>
 * For generic UI display of the API version, {@link #getApiString()} is to be used.
 */
public final class AndroidVersion implements Comparable<AndroidVersion> {

    private final int mApiLevel;
    private final String mCodename;

    /**
     * Creates an {@link AndroidVersion} with the given api level and codename.
     * Codename should be null for a release version, otherwise it's a preview codename.
     *
     * @param apiLevel The API level of the Android version.
     * @param codename The codename of the Android version, or null for a release version.
     */
    public AndroidVersion(int apiLevel, @Nullable String codename) {
        mApiLevel = apiLevel;
        mCodename = sanitizeCodename(codename);
    }

    /**
     * Creates an {@link AndroidVersion} from a string that may be an integer API
     * level or a string codename.
     * <p>
     * <Em>Important</em>: An important limitation of this method is that cannot possibly
     * recreate the API level integer from a pure string codename. This is only OK to use
     * if the caller can guarantee that only {@link #getApiString()} will be used later.
     * Wrong things will happen if the caller then tries to resolve the numeric
     * {@link #getApiLevel()}.
     *
     * @param apiOrCodename A non-null API integer or a codename in its "ALL_CAPS" format.
     *                      "REL" is notable not a valid codename.
     * @throws AndroidVersionException if the input isn't a pure integer or doesn't look like
     *                                 a valid string codename.
     */
    public AndroidVersion(@NonNull String apiOrCodename) throws AndroidVersionException {
        int apiLevel = 0;
        String codename = null;
        try {
            apiLevel = Integer.parseInt(apiOrCodename);
        } catch (NumberFormatException ignore) {
            // We don't know the API level. Android platform codenames are all caps.
            // REL is a release-reserved keyword which we can use here.

            if (!SdkConstants.CODENAME_RELEASE.equals(apiOrCodename) &&
                    Pattern.matches("[A-Z_]+", apiOrCodename)) {
                codename = apiOrCodename;
            }

        }

        mApiLevel = apiLevel;
        mCodename = sanitizeCodename(codename);

        if (mApiLevel <= 0 && codename == null) {
            throw new AndroidVersionException(
                    "Invalid android API or codename " + apiOrCodename,     //$NON-NLS-1$
                    null);
        }
    }

    /**
     * Sanitizes the codename string according to the following rules:
     * - A codename should be {@code null} for a release version, or it should be a non-empty
     * string for an actual preview.
     * - In input, spacing is trimmed since it is irrelevant.
     * - An empty string or the special codename "REL" means a release version
     * and is converted to {@code null}.
     *
     * @param codename A possible-null codename.
     * @return Null for a release version or a non-empty codename.
     */
    @Nullable
    private static String sanitizeCodename(@Nullable String codename) {
        if (codename != null) {
            codename = codename.trim();
            if (codename.isEmpty() || SdkConstants.CODENAME_RELEASE.equals(codename)) {
                codename = null;
            }
        }
        return codename;
    }

    /**
     * Returns the api level as an integer.
     * <p>To display the API level in the UI, use {@link #getApiString()}, which will use the
     * codename if applicable.
     *
     * @return the API level of this version
     * @see #getApiString()
     */
    public int getApiLevel() {
        return mApiLevel;
    }

    /**
     * Returns a string representing the API level and/or the code name.
     *
     * @return a non-null string
     */
    @NonNull
    public String getApiString() {
        if (mCodename != null) {
            return mCodename;
        }

        return Integer.toString(mApiLevel);
    }

    /**
     * Returns whether the version is a preview version.
     *
     * @return true if the version is a preview version
     */
    public boolean isPreview() {
        return mCodename != null;
    }

    /**
     * Compares the receiver with either an {@link AndroidVersion} object or a {@link String}
     * object.
     * <p>If <var>obj</var> is a {@link String}, then the method will first check if it's a string
     * representation of a number, in which case it'll compare it to the api level. Otherwise, it'll
     * compare it against the code name.
     * <p>For all other type of object give as parameter, this method will return
     * <code>false</code>.
     *
     * @return true if the receiver is equal to the given object.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AndroidVersion version) {

            if (mCodename == null) {
                return version.mCodename == null &&
                        mApiLevel == version.mApiLevel;
            } else {
                return mCodename.equals(version.mCodename) &&
                        mApiLevel == version.mApiLevel;
            }

        } else if (obj instanceof String str) {
            // if we have a code name, this must match.
            if (mCodename != null) {
                return mCodename.equals(obj);
            }

            // else we try to convert to an int and compare to the api level
            try {
                int value = Integer.parseInt(str);
                return value == mApiLevel;
            } catch (NumberFormatException e) {
                // not a number? we'll return false below.
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        if (mCodename != null) {
            return mCodename.hashCode();
        }

        // there may be some collisions between the hashcode of the codename and the api level,
        // but it's acceptable.
        return mApiLevel;
    }

    /**
     * Returns a string with the API Level and optional codename.
     * Useful for debugging.
     * For display purpose, please use {@link #getApiString()} instead.
     *
     * @return a non-null string
     */
    @Override
    public String toString() {
        String s = String.format("API %1$d", mApiLevel);        //$NON-NLS-1$
        if (isPreview()) {
            s += String.format(", %1$s preview", mCodename);    //$NON-NLS-1$
        }
        return s;
    }

    /**
     * Compares this object with the specified object for order. Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * @param o the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object is
     * less than, equal to, or greater than the specified object.
     */
    @Override
    public int compareTo(@NonNull AndroidVersion o) {
        return compareTo(o.mApiLevel, o.mCodename);
    }

    /**
     * @param apiLevel the api level to compare to
     * @param codename the codename to compare to. Can be null.
     * @return a negative integer, zero, or a positive integer as this object is
     * less than, equal to, or greater than the specified object.
     */
    public int compareTo(int apiLevel, @Nullable String codename) {
        if (mCodename == null) {
            if (codename != null && mApiLevel == apiLevel) {
                return -1; // same api level but argument is a preview for next version
            }
            return mApiLevel - apiLevel;
        } else {
            // 'this' is a preview
            if (mApiLevel == apiLevel) {
                if (codename == null) {
                    return +1;
                } else {
                    return mCodename.compareTo(codename);    // strange case where the 2 previews
                    // have different codename?
                }
            } else {
                return mApiLevel - apiLevel;
            }
        }
    }

    /**
     * Thrown when an {@link AndroidVersion} object could not be created.
     */
    public static final class AndroidVersionException extends Exception {
        @Serial
        private static final long serialVersionUID = 1L;

        AndroidVersionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
