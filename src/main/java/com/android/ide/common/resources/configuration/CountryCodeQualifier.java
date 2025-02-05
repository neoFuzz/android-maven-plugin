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
import com.android.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resource Qualifier for Mobile Country Code.
 */
public final class CountryCodeQualifier extends ResourceQualifier {
    /**
     * Constant for the mobile country code qualifier name.
     */
    public static final String NAME = "Mobile Country Code";
    /**
     * Default pixel density value. This means the property is not set.
     */
    private static final int DEFAULT_CODE = -1;
    /**
     * Pattern for matching folder segment containing the qualifier.
     */
    private static final Pattern sCountryCodePattern = Pattern.compile("^mcc(\\d{3})$");//$NON-NLS-1$
    /**
     * Constant for the mobile country code qualifier.
     */
    private final int mCode;

    /**
     * Constructor that creates a new qualifier and sets its value.
     */
    public CountryCodeQualifier() {
        this(DEFAULT_CODE);
    }

    /**
     * @param code the value of the qualifier
     */
    public CountryCodeQualifier(int code) {
        mCode = code;
    }

    /**
     * Creates and returns a qualifier from the given folder segment. If the segment is incorrect,
     * <code>null</code> is returned.
     *
     * @param segment the folder segment from which to create a qualifier.
     * @return a new {@link CountryCodeQualifier} object or <code>null</code>
     */
    @Nullable
    public static CountryCodeQualifier getQualifier(String segment) {
        Matcher m = sCountryCodePattern.matcher(segment);
        if (m.matches()) {
            String v = m.group(1);

            int code;
            try {
                code = Integer.parseInt(v);
            } catch (NumberFormatException e) {
                // looks like the string we extracted wasn't a valid number.
                return null;
            }

            return new CountryCodeQualifier(code);
        }

        return null;
    }

    /**
     * Returns the folder name segment for the given value. This is equivalent to calling
     * {@link #toString()} on a {@link CountryCodeQualifier} object.
     *
     * @param code the value of the qualifier, as returned by {@link #getCode()}.
     * @return the folder segment
     */
    @NonNull
    public static String getFolderSegment(int code) {
        if (code >= 100 && code <= 999) { // code is 3 digit.
            return String.format("mcc%1$d", code); //$NON-NLS-1$
        }

        return ""; //$NON-NLS-1$
    }

    /**
     * @return the code value of this qualifier
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
     * @return the short name of the qualifier
     */
    @Override
    @NonNull
    public String getShortName() {
        return "Country Code";
    }

    /**
     * @return since version of the qualifier
     */
    @Override
    public int since() {
        return 1;
    }

    /**
     * @return true if the qualifier is valid, false otherwise
     */
    @Override
    public boolean isValid() {
        return mCode != DEFAULT_CODE;
    }

    /**
     * @return true if the qualifier has a fake value, false otherwise
     */
    @Override
    public boolean hasFakeValue() {
        return false;
    }

    /**
     * @param value  The value to check and set. Must not be null.
     * @param config The folder configuration to receive the value. Must not be null.
     * @return true if the value was set, false otherwise
     */
    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        CountryCodeQualifier qualifier = getQualifier(value);
        if (qualifier != null) {
            config.setCountryCodeQualifier(qualifier);
            return true;
        }

        return false;
    }

    /**
     * @param qualifier The qualifier to compare with. Must not be null.
     * @return true if the given qualifier is equal to this qualifier, false otherwise
     */
    @Override
    public boolean equals(Object qualifier) {
        if (qualifier instanceof CountryCodeQualifier q) {
            return mCode == q.mCode;
        }

        return false;
    }

    /**
     * @return the hash code value of this qualifier
     */
    @Override
    public int hashCode() {
        return mCode;
    }

    /**
     * Returns the string used to represent this qualifier in the folder name.
     *
     * @return the string used to represent this qualifier in the folder name.
     */
    @Override
    @NonNull
    public String getFolderSegment() {
        return getFolderSegment(mCode);
    }

    /**
     * @return the display value of this qualifier
     */
    @Override
    @NonNull
    public String getShortDisplayValue() {
        if (mCode != DEFAULT_CODE) {
            return String.format("MCC %1$d", mCode);
        }

        return ""; //$NON-NLS-1$
    }

    /**
     * @return the long display value of this qualifier
     */
    @Override
    @NonNull
    public String getLongDisplayValue() {
        return getShortDisplayValue();
    }

}
