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

import com.android.resources.NavigationState;
import com.android.resources.ResourceEnum;

/**
 * Resource Qualifier for navigation state.
 */
public final class NavigationStateQualifier extends EnumBasedResourceQualifier {

    /**
     * The qualifier name for navigation state
     */
    public static final String NAME = "Navigation State";

    /**
     * The navigation state value
     */
    private NavigationState mValue = null;

    /**
     * Default constructor
     */
    public NavigationStateQualifier() {
        // pass
    }

    /**
     * @param value the value to set
     */
    public NavigationStateQualifier(NavigationState value) {
        mValue = value;
    }

    /**
     * @return the navigation state value
     */
    public NavigationState getValue() {
        return mValue;
    }

    /**
     * @return the navigation state enum value@return
     */
    @Override
    ResourceEnum getEnumValue() {
        return mValue;
    }

    /**
     * @return the folder name for the navigation state
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * @return the folder name for the navigation state
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
     * @return true if the value was set
     */
    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        NavigationState state = NavigationState.getEnum(value);
        if (state != null) {
            NavigationStateQualifier qualifier = new NavigationStateQualifier();
            qualifier.mValue = state;
            config.setNavigationStateQualifier(qualifier);
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
        if (qualifier instanceof NavigationStateQualifier q) {
            return mValue == q.mValue;
        }

        return false;
    }

    /**
     * @return the hash code for the navigation state value
     */
    @Override
    public int hashCode() {
        return mValue.hashCode();
    }
}
