/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.utils;

import com.android.annotations.NonNull;
import com.google.common.base.CaseFormat;

/**
 * Miscellaneous utilities used by the Android SDK tools
 */
public class SdkUtils {

    private SdkUtils() {
        // empty
    }

    /**
     * Returns true if the given string ends with the given suffix, using a
     * case-insensitive comparison.
     *
     * @param string the full string to be checked
     * @param suffix the suffix to be checked for
     * @return true if the string case-insensitively ends with the given suffix
     */
    public static boolean endsWithIgnoreCase(@NonNull String string, @NonNull String suffix) {
        return string.regionMatches(true /* ignoreCase */, string.length() - suffix.length(),
                suffix, 0, suffix.length());
    }

    /**
     * Returns true if the given sequence ends with the given suffix (case-     * sensitive).
     *
     * @param sequence the character sequence to be checked
     * @param suffix   the suffix to look for
     * @return true if the given sequence ends with the given suffix
     */
    public static boolean endsWith(@NonNull CharSequence sequence, @NonNull CharSequence suffix) {
        return endsWith(sequence, sequence.length(), suffix);
    }

    /**
     * Returns true if the given sequence ends at the given offset with the given suffix (case-     * sensitive)
     *
     * @param sequence  the character sequence to be checked
     * @param endOffset the offset at which the sequence is considered to end
     * @param suffix    the suffix to look for
     * @return true if the given sequence ends with the given suffix
     */
    public static boolean endsWith(@NonNull CharSequence sequence, int endOffset,
                                   @NonNull CharSequence suffix) {
        if (endOffset < suffix.length()) {
            return false;
        }
        for (int i = endOffset - 1, j = suffix.length() - 1; j >= 0; i--, j--) {
            if (sequence.charAt(i) != suffix.charAt(j)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Translates an XML name (e.g. xml-name) into a Java / C++ constant name (e.g. XML_NAME)
     *
     * @param xmlName the hyphen separated lower case xml name.
     * @return the equivalent constant name.
     */
    @NonNull
    public static String xmlNameToConstantName(String xmlName) {
        return CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_UNDERSCORE, xmlName);
    }

    /**
     * Translates a camel case name (e.g. xmlName) into a Java / C++ constant name (e.g. XML_NAME)
     *
     * @param camelCaseName the camel case name.
     * @return the equivalent constant name.
     */
    @NonNull
    public static String camelCaseToConstantName(String camelCaseName) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, camelCaseName);
    }

    /**
     * Translates a Java / C++ constant name (e.g. XML_NAME) into camel case name (e.g. xmlName)
     *
     * @param constantName the constant name.
     * @return the equivalent camel case name.
     */
    @NonNull
    public static String constantNameToCamelCase(String constantName) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, constantName);
    }

    /**
     * Translates a Java / C++ constant name (e.g. XML_NAME) into a XML case name (e.g. xml-name)
     *
     * @param constantName the constant name.
     * @return the equivalent XML name.
     */
    @NonNull
    public static String constantNameToXmlName(String constantName) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, constantName);
    }

}