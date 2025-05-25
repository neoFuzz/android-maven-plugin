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
package com.android.sdklib;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

import java.util.Locale;

/**
 * Information about available SDK Versions
 */
public class SdkVersionInfo {
    private SdkVersionInfo() {
        // Not instantiable
    }

    /**
     * The highest known API level. Note that the tools may also look at the
     * installed platforms to see if they can find more recently released
     * platforms, e.g. when the tools have not yet been updated for a new
     * release. This number is used as a baseline and any more recent platforms
     * found can be used to increase the highest known number.
     */
    public static final int HIGHEST_KNOWN_API = 23;

    /**
     * Returns the applicable build code (for
     * {@code android.os.Build.VERSION_CODES}) for the corresponding API level,
     * or null if it's unknown. The highest number (inclusive) that is supported
     * is {@link SdkVersionInfo#HIGHEST_KNOWN_API}.
     *
     * @param api the API level to look up a version code for
     * @return the corresponding build code field name, or null
     */
    @Nullable
    public static String getBuildCode(int api) {
        // See http://developer.android.com/reference/android/os/Build.VERSION_CODES.html
        return switch (api) {
            case 1 -> "BASE"; //$NON-NLS-1$
            case 2 -> "BASE_1_1"; //$NON-NLS-1$
            case 3 -> "CUPCAKE"; //$NON-NLS-1$
            case 4 -> "DONUT"; //$NON-NLS-1$
            case 5 -> "ECLAIR"; //$NON-NLS-1$
            case 6 -> "ECLAIR_0_1"; //$NON-NLS-1$
            case 7 -> "ECLAIR_MR1"; //$NON-NLS-1$
            case 8 -> "FROYO"; //$NON-NLS-1$
            case 9 -> "GINGERBREAD"; //$NON-NLS-1$
            case 10 -> "GINGERBREAD_MR1"; //$NON-NLS-1$
            case 11 -> "HONEYCOMB"; //$NON-NLS-1$
            case 12 -> "HONEYCOMB_MR1"; //$NON-NLS-1$
            case 13 -> "HONEYCOMB_MR2"; //$NON-NLS-1$
            case 14 -> "ICE_CREAM_SANDWICH"; //$NON-NLS-1$
            case 15 -> "ICE_CREAM_SANDWICH_MR1"; //$NON-NLS-1$
            case 16 -> "JELLY_BEAN"; //$NON-NLS-1$
            case 17 -> "JELLY_BEAN_MR1"; //$NON-NLS-1$
            case 18 -> "JELLY_BEAN_MR2"; //$NON-NLS-1$
            case 19 -> "KITKAT"; //$NON-NLS-1$
            case 20 -> "KITKAT_WATCH"; //$NON-NLS-1$
            case 21 -> "LOLLIPOP"; //$NON-NLS-1$
            case 22 -> "LOLLIPOP_MR1"; //$NON-NLS-1$
            case 23 -> "MNC";
            default -> //$NON-NLS-1$
                // If you add more versions here, also update #getAndroidName and
                // #HIGHEST_KNOWN_API
                    null;
        };

    }

    /**
     * Returns the API level of the given build code (e.g. JELLY_BEAN_MR1 => 17), or -1 if not
     * recognized
     *
     * @param buildCode         the build code name (not case-sensitive)
     * @param recognizeUnknowns if true, treat an unrecognized code name as a newly released
     *                          platform the tools are not yet aware of, and set its API level to
     *                          some higher number than all the currently known API versions
     * @return the API level, or -1 if not recognized (unless recognizeUnknowns is true, in which
     * {@link #HIGHEST_KNOWN_API} plus one is returned
     */
    public static int getApiByBuildCode(@NonNull String buildCode, boolean recognizeUnknowns) {
        for (int api = 1; api <= HIGHEST_KNOWN_API; api++) {
            String code = getBuildCode(api);
            if (code != null && code.equalsIgnoreCase(buildCode)) {
                return api;
            }
        }

        if (buildCode.equalsIgnoreCase("L")) {
            return 21; // For now the Build class also provides this as an alias to Lollipop
        }

        return recognizeUnknowns ? HIGHEST_KNOWN_API + 1 : -1;
    }

    /**
     * Returns the API level of the given preview code name (e.g. JellyBeanMR2 => 17), or -1 if not
     * recognized
     *
     * @param previewName       the preview name (not case-sensitive)
     * @param recognizeUnknowns if true, treat an unrecognized code name as a newly released
     *                          platform the tools are not yet aware of, and set its API level to
     *                          some higher number than all the currently known API versions
     * @return the API level, or -1 if not recognized (unless recognizeUnknowns is true, in which
     * {@link #HIGHEST_KNOWN_API} plus one is returned
     */
    public static int getApiByPreviewName(@NonNull String previewName, boolean recognizeUnknowns) {
        // JellyBean => JELLY_BEAN
        String codeName = camelCaseToUnderlines(previewName).toUpperCase(Locale.US);
        return getApiByBuildCode(codeName, recognizeUnknowns);
    }

    /**
     * Converts a CamelCase word into an underlined_word
     *
     * @param string the CamelCase version of the word
     * @return the underlined version of the word
     */
    @NonNull
    public static String camelCaseToUnderlines(@NonNull String string) {
        if (string.isEmpty()) {
            return string;
        }

        StringBuilder sb = new StringBuilder(2 * string.length());
        int n = string.length();
        boolean lastWasUpperCase = Character.isUpperCase(string.charAt(0));
        for (int i = 0; i < n; i++) {
            char c = string.charAt(i);
            boolean isUpperCase = Character.isUpperCase(c);
            if (isUpperCase && !lastWasUpperCase) {
                sb.append('_');
            }
            lastWasUpperCase = isUpperCase;
            c = Character.toLowerCase(c);
            sb.append(c);
        }

        return sb.toString();
    }

}
