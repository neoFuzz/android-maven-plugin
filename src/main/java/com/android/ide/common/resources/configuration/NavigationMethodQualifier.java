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

import com.android.resources.Navigation;
import com.android.resources.ResourceEnum;

/**
 * Resource Qualifier for Navigation Method.
 */
public final class NavigationMethodQualifier extends EnumBasedResourceQualifier {

    /**
     * The qualifier name for navigation method
     */
    public static final String NAME = "Navigation Method";

    /**
     * The enum value for navigation method
     */
    private Navigation mValue;

    /**
     * Default constructor
     * Initializes the qualifier with an empty value
     */
    public NavigationMethodQualifier() {
        // pass
    }

    /**
     * @param value the enum value for navigation method
     */
    public NavigationMethodQualifier(Navigation value) {
        mValue = value;
    }

    /**
     * @return the enum value for navigation method
     */
    public Navigation getValue() {
        return mValue;
    }

    /**
     * @return the enum value for navigation method
     */
    @Override
    ResourceEnum getEnumValue() {
        return mValue;
    }

    /**
     * @return the folder name for navigation method
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * @return the folder name for navigation method
     */
    @Override
    public String getShortName() {
        return NAME;
    }

    /**
     * @return only 1
     */
    @Override
    public int since() {
        return 1;
    }

    /**
     * @param value  The value to check and set. Must not be null.
     * @param config The folder configuration to receive the value. Must not be null.
     * @return true if the value was set successfully, false otherwise
     */
    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        Navigation method = Navigation.getEnum(value);
        if (method != null) {
            NavigationMethodQualifier qualifier = new NavigationMethodQualifier(method);
            config.setNavigationMethodQualifier(qualifier);
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
        if (qualifier instanceof NavigationMethodQualifier q) {
            return mValue == q.mValue;
        }

        return false;
    }

    /**
     * @return the hash code for the navigation method value
     */
    @Override
    public int hashCode() {
        return mValue.hashCode();
    }
}
