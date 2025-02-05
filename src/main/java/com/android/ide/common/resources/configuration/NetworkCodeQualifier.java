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

package com.android.ide.common.resources.configuration;

import com.android.annotations.NonNull;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resource Qualifier for Mobile Network Code Pixel Density.
 */
public final class NetworkCodeQualifier extends ResourceQualifier {
    /**
     * Pattern for matching folder segment of the form mncNNN.
     */
    public static final String NAME = "Mobile Network Code";
    /**
     * Default pixel density value. This means the property is not set.
     */
    private static final int DEFAULT_CODE = -1;
    /**
     * Pattern for matching folder segment of the form mncNNN.
     */
    private static final Pattern sNetworkCodePattern = Pattern.compile("^mnc(\\d{1,3})$"); //$NON-NLS-1$
    /**
     * The value of the qualifier.
     */
    private final int mCode;

    /**
     * Constructor
     */
    public NetworkCodeQualifier() {
        this(DEFAULT_CODE);
    }

    /**
     * @param code the code value for the qualifier
     */
    public NetworkCodeQualifier(int code) {
        mCode = code;
    }

    /**
     * Creates and returns a qualifier from the given folder segment. If the segment is incorrect,
     * <code>null</code> is returned.
     *
     * @param segment the folder segment from which to create a qualifier.
     * @return a new {@link CountryCodeQualifier} object or <code>null</code>
     */
    @NonNull
    public static NetworkCodeQualifier getQualifier(String segment) {
        Matcher m = sNetworkCodePattern.matcher(segment);
        if (m.matches()) {
            String v = m.group(1);

            int code;
            try {
                code = Integer.parseInt(v);
            } catch (NumberFormatException e) {
                // looks like the string we extracted wasn't a valid number.
                return null;
            }

            return new NetworkCodeQualifier(code);
        }

        return null;
    }

    /**
     * Returns the folder name segment for the given value. This is equivalent to calling
     * {@link #toString()} on a {@link NetworkCodeQualifier} object.
     *
     * @param code the value of the qualifier, as returned by {@link #getCode()}.
     * @return the folder segment for the given value
     */
    @NonNull
    public static String getFolderSegment(int code) {
        if (code != DEFAULT_CODE && code >= 1 && code <= 999) { // code is 1-3 digit.
            return String.format(Locale.US, "mnc%1$03d", code); //$NON-NLS-1$
        }

        return ""; //$NON-NLS-1$
    }

    /**
     * @return the code value of the qualifier
     */
    public int getCode() {
        return mCode;
    }

    /**
     * @return the name of the qualifier
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * @return the name of the qualifier
     */
    @Override
    @NonNull
    public String getShortName() {
        return "Network Code";
    }

    /**
     * @return the lowest possible API level in which this qualifier can be used
     */
    @Override
    public int since() {
        return 1;
    }

    /**
     * @return true if the qualifier has been correctly set
     */
    @Override
    public boolean isValid() {
        return mCode != DEFAULT_CODE;
    }

    /**
     * @return true if the qualifier has a fake value
     */
    @Override
    public boolean hasFakeValue() {
        return false;
    }

    /**
     * @param value  The value to check and set. Must not be null.
     * @param config The folder configuration to receive the value. Must not be null.
     * @return true if the value was accepted and set
     */
    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        Matcher m = sNetworkCodePattern.matcher(value);
        if (m.matches()) {
            String v = m.group(1);

            int code;
            try {
                code = Integer.parseInt(v);
            } catch (NumberFormatException e) {
                // looks like the string we extracted wasn't a valid number.
                return false;
            }

            NetworkCodeQualifier qualifier = new NetworkCodeQualifier(code);
            config.setNetworkCodeQualifier(qualifier);
            return true;
        }

        return false;
    }

    /**
     * @param qualifier The qualifier to compare. Must not be null.
     * @return true if the qualifier is compatible with this object
     */
    @Override
    public boolean equals(Object qualifier) {
        if (qualifier instanceof NetworkCodeQualifier nc) {
            return mCode == nc.mCode;
        }

        return false;
    }

    /**
     * @return the hash code for the qualifier's value
     */
    @Override
    public int hashCode() {
        return mCode;
    }

    /**
     * Returns the string used to represent this qualifier in the folder name.
     */
    @Override
    @NonNull
    public String getFolderSegment() {
        return getFolderSegment(mCode);
    }

    /**
     * @return a string representation of the qualifier
     */
    @Override
    @NonNull
    public String getShortDisplayValue() {
        if (mCode != DEFAULT_CODE) {
            return String.format("MNC %1$d", mCode);
        }

        return ""; //$NON-NLS-1$
    }

    /**
     * @return a string representation of the qualifier
     */
    @Override
    @NonNull
    public String getLongDisplayValue() {
        return getShortDisplayValue();
    }

}
