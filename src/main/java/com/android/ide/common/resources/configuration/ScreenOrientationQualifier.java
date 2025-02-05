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
import com.android.resources.ResourceEnum;
import com.android.resources.ScreenOrientation;

/**
 * Resource Qualifier for Screen Orientation.
 */
public final class ScreenOrientationQualifier extends EnumBasedResourceQualifier {

    /**
     * String name of the resource being qualified
     */
    public static final String NAME = "Screen Orientation";

    private ScreenOrientation mValue = null;

    /**
     * Default constructor
     */
    public ScreenOrientationQualifier() {
    }

    /**
     * @param value the screen orientation value as a {@link ScreenOrientation} enum
     */
    public ScreenOrientationQualifier(ScreenOrientation value) {
        mValue = value;
    }

    /**
     * @return the screen orientation value as a {@link ScreenOrientation} enum
     */
    public ScreenOrientation getValue() {
        return mValue;
    }

    @Override
    ResourceEnum getEnumValue() {
        return mValue;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    @NonNull
    public String getShortName() {
        return "Orientation";
    }

    @Override
    public int since() {
        return 1;
    }

    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        ScreenOrientation orientation = ScreenOrientation.getEnum(value);
        if (orientation != null) {
            ScreenOrientationQualifier qualifier = new ScreenOrientationQualifier(orientation);
            config.setScreenOrientationQualifier(qualifier);
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object qualifier) {
        return qualifier instanceof ScreenOrientationQualifier s &&
                s.mValue == mValue;
    }

    @Override
    public int hashCode() {
        return mValue.hashCode();
    }
}
