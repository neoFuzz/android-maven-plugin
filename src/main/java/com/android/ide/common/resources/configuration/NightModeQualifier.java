/*
 * Copyright (C) 2010 The Android Open Source Project
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

import com.android.resources.NightMode;
import com.android.resources.ResourceEnum;

/**
 * Resource Qualifier for Navigation Method.
 */
public final class NightModeQualifier extends EnumBasedResourceQualifier {

    /**
     * String for night modes.
     */
    public static final String NAME = "Night Mode";

    /**
     * The value of the qualifier
     */
    private NightMode mValue;

    /**
     * Default constructor. value is default value.
     */
    public NightModeQualifier() {
        // pass
    }

    /**
     * @param value the value to set
     */
    public NightModeQualifier(NightMode value) {
        mValue = value;
    }

    /**
     * @return the enum value for night mode
     */
    public NightMode getValue() {
        return mValue;
    }

    /**
     * @return the enum value for night mode
     */
    @Override
    ResourceEnum getEnumValue() {
        return mValue;
    }

    /**
     * @return the folder name for night mode
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * @return the folder name for night mode
     */
    @Override
    public String getShortName() {
        return NAME;
    }

    /**
     * @return always 8
     */
    @Override
    public int since() {
        return 8;
    }

    /**
     * @param value  The value to check and set. Must not be null.
     * @param config The folder configuration to receive the value. Must not be null.
     * @return true if the value was set
     */
    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        NightMode mode = NightMode.getEnum(value);
        if (mode != null) {
            NightModeQualifier qualifier = new NightModeQualifier(mode);
            config.setNightModeQualifier(qualifier);
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
        return qualifier instanceof NightModeQualifier nm &&
                nm.mValue == mValue;
    }

    /**
     * @return the hash code for the night mode value
     */
    @Override
    public int hashCode() {
        return mValue.hashCode();
    }
}
