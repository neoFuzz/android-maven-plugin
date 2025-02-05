/*
 * Copyright (C) 2007 The Android Open Source Project
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

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Resource Qualifier for Language.
 */
public final class LanguageQualifier extends ResourceQualifier {
    /**
     * Constant for the fake language value
     */
    public static final String FAKE_LANG_VALUE = "__"; //$NON-NLS-1$
    /**
     * Name of the qualifier
     */
    public static final String NAME = "Language";
    /**
     * Pattern for language validation
     */
    private static final Pattern sLanguagePattern = Pattern.compile("^[a-zA-Z]{2}$"); //$NON-NLS-1$
    /**
     * Value of the language qualifier
     */
    private String mValue;

    /**
     * Constructor
     */
    public LanguageQualifier() {
        // Nothing to do.
    }

    /**
     * Creates and returns a qualifier from the given folder segment. If the segment is incorrect,
     * <code>null</code> is returned.
     *
     * @param segment the folder segment from which to create a qualifier.
     * @return a new {@link LanguageQualifier} object or <code>null</code>
     */
    @Nullable
    public static LanguageQualifier getQualifier(String segment) {
        if (sLanguagePattern.matcher(segment).matches()) {
            LanguageQualifier qualifier = new LanguageQualifier();
            qualifier.mValue = segment;

            return qualifier;
        }
        return null;
    }

    /**
     * Returns the folder name segment for the given value. This is equivalent to calling
     * {@link #toString()} on a {@link LanguageQualifier} object.
     *
     * @param value the value of the qualifier, as returned by {@link #getValue()}.
     * @return the string for the folder segment
     */
    @Nullable
    public static String getFolderSegment(@NonNull String value) {
        String segment = value.toLowerCase(Locale.US);
        if (sLanguagePattern.matcher(segment).matches()) {
            return segment;
        }

        return null;
    }

    /**
     * @return the language value
     */
    public String getValue() {
        return Objects.requireNonNullElse(mValue, "");
    }

    /**
     * @return the language value
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * @return the language value
     */
    @Override
    public String getShortName() {
        return NAME;
    }

    /**
     * @return 1
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
        return mValue != null;
    }

    /**
     * @return true if the qualifier has a fake value
     */
    @Override
    public boolean hasFakeValue() {
        return FAKE_LANG_VALUE.equals(mValue);
    }

    /**
     * @param value  The value to check and set. Must not be null.
     * @param config The folder configuration to receive the value. Must not be null.
     * @return true if the value was accepted
     */
    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        LanguageQualifier qualifier = getQualifier(value);
        if (qualifier != null) {
            config.setLanguageQualifier(qualifier);
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
        if (qualifier instanceof LanguageQualifier lq) {
            if (mValue == null) {
                return lq.mValue == null;
            }
            return mValue.equals(lq.mValue);
        }

        return false;
    }

    /**
     * @return the hash code for the language value
     */
    @Override
    public int hashCode() {
        if (mValue != null) {
            return mValue.hashCode();
        }

        return 0;
    }

    /**
     * Returns the string used to represent this qualifier in the folder name.
     *
     * @return the string used to represent this qualifier in the folder name.
     */
    @Override
    public String getFolderSegment() {
        if (mValue != null) {
            return getFolderSegment(mValue);
        }

        return ""; //$NON-NLS-1$
    }

    /**
     * @return the string to display to the user
     */
    @Override
    public String getShortDisplayValue() {
        return Objects.requireNonNullElse(mValue, "");

    }

    /**
     * @return the string to display to the user
     */
    @Override
    @NonNull
    public String getLongDisplayValue() {
        if (mValue != null) {
            return String.format("Language %s", mValue);
        }

        return ""; //$NON-NLS-1$
    }
}
