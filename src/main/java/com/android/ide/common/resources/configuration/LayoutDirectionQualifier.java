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

package com.android.ide.common.resources.configuration;

import com.android.resources.LayoutDirection;
import com.android.resources.ResourceEnum;

/**
 * Resource Qualifier for layout direction. values can be "ltr", or "rtl"
 */
public class LayoutDirectionQualifier extends EnumBasedResourceQualifier {

    /**
     * Static string for layout direction qualifier
     */
    public static final String NAME = "Layout Direction";

    /**
     * The layout direction value
     */
    private LayoutDirection mValue = null;

    /**
     * Default constructor. The value is set to {@link LayoutDirection#LTR}
     */
    public LayoutDirectionQualifier() {
    }

    /**
     * Constructor with a {@link LayoutDirection} value.
     *
     * @param value the layout direction value
     */
    public LayoutDirectionQualifier(LayoutDirection value) {
        mValue = value;
    }

    /**
     * @return the layout direction value
     */
    public LayoutDirection getValue() {
        return mValue;
    }

    /**
     * @return the layout direction enum value
     */
    @Override
    ResourceEnum getEnumValue() {
        return mValue;
    }

    /**
     * @return the folder name for the layout direction
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * @return the folder name for the layout direction
     */
    @Override
    public String getShortName() {
        return NAME;
    }

    /**
     * @return the folder name for the layout direction
     */
    @Override
    public int since() {
        return 17;
    }

    /**
     * @param value  The value to check and set. Must not be null.
     * @param config The folder configuration to receive the value. Must not be null.
     * @return true if the value was accepted
     */
    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        LayoutDirection ld = LayoutDirection.getEnum(value);
        if (ld != null) {
            LayoutDirectionQualifier qualifier = new LayoutDirectionQualifier(ld);
            config.setLayoutDirectionQualifier(qualifier);
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
        return qualifier instanceof LayoutDirectionQualifier l && mValue == l.mValue;
    }

    /**
     * @return the hash code for the layout direction value
     */
    @Override
    public int hashCode() {
        return mValue.hashCode();
    }
}
