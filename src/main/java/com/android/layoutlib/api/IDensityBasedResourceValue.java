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

package com.android.layoutlib.api;

import com.android.annotations.Nullable;
import com.android.ide.common.rendering.api.DensityBasedResourceValue;

/**
 * Represents an Android Resources that has a density info attached to it.
 *
 * @deprecated use {@link DensityBasedResourceValue}.
 */
@Deprecated
public interface IDensityBasedResourceValue extends IResourceValue {

    /**
     * Returns the density associated to the resource.
     *
     * @return the density associated to the resource.
     * @deprecated use {@link DensityBasedResourceValue#getResourceDensity()}
     */
    @Deprecated
    Density getDensity();

    /**
     * Density.
     *
     * @deprecated use {@link com.android.resources.Density}.
     */
    @Deprecated
    enum Density {
        /**
         * Extra high density (xhdpi)
         */
        XHIGH(320),
        /**
         * High density (hdpi)
         */
        HIGH(240),
        /**
         * Medium density (mdpi)
         */
        MEDIUM(160),
        /**
         * Low density (ldpi)
         */
        LOW(120),
        /**
         * No density (for text resources)
         */
        NODPI(0);

        /**
         * The density value.
         */
        private final int mValue;

        /**
         * @param value The density value.
         */
        Density(int value) {
            mValue = value;
        }

        /**
         * Returns the enum matching the given density value
         *
         * @param value The density value.
         * @return the enum for the density value or null if no match was found.
         */
        @Nullable
        public static Density getEnum(int value) {
            for (Density d : values()) {
                if (d.mValue == value) {
                    return d;
                }
            }

            return null;
        }

        /**
         * @return the int value of the density
         */
        public int getValue() {
            return mValue;
        }
    }
}
