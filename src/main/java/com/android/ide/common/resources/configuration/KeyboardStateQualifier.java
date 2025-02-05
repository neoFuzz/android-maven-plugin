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
import com.android.resources.KeyboardState;
import com.android.resources.ResourceEnum;

/**
 * Resource Qualifier for keyboard state.
 */
public final class KeyboardStateQualifier extends EnumBasedResourceQualifier {

    /**
     * Static string for keyboard state qualifier
     */
    public static final String NAME = "Keyboard State";

    /**
     * The value of the qualifier
     */
    private KeyboardState mValue = null;

    /**
     * Default constructor
     */
    public KeyboardStateQualifier() {
        // pass
    }

    /**
     * @param value the value of the qualifier
     */
    public KeyboardStateQualifier(KeyboardState value) {
        mValue = value;
    }

    /**
     * @return the value of the qualifier
     */
    public KeyboardState getValue() {
        return mValue;
    }

    /**
     * @return the resource type for keyboard state qualifier
     */
    @Override
    ResourceEnum getEnumValue() {
        return mValue;
    }

    /**
     * @return the folder name for keyboard state qualifier
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * @return the folder name for keyboard state qualifier
     */
    @Override
    @NonNull
    public String getShortName() {
        return "Keyboard";
    }

    /**
     * @return the folder name for keyboard state qualifier, always 1
     */
    @Override
    public int since() {
        return 1;
    }

    /**
     * @param value  The value to check and set. Must not be null.
     * @param config The folder configuration to receive the value. Must not be null.
     * @return true if the value was set, false otherwise
     */
    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        KeyboardState orientation = KeyboardState.getEnum(value);
        if (orientation != null) {
            KeyboardStateQualifier qualifier = new KeyboardStateQualifier();
            qualifier.mValue = orientation;
            config.setKeyboardStateQualifier(qualifier);
            return true;
        }

        return false;
    }

    /**
     * @param qualifier the reference qualifier
     * @return true if the reference qualifier is a match for this qualifier
     */
    @Override
    public boolean isMatchFor(ResourceQualifier qualifier) {
        if (qualifier instanceof KeyboardStateQualifier referenceQualifier) {
            // special case where EXPOSED can be used for SOFT
            if (referenceQualifier.mValue == KeyboardState.SOFT &&
                    mValue == KeyboardState.EXPOSED) {
                return true;
            }

            return referenceQualifier.mValue == mValue;
        }

        return false;
    }

    /**
     * @param compareTo The {@link ResourceQualifier} to compare to. Can be null, in which
     *                  case the method must return <code>true</code>.
     * @param reference The reference qualifier value for which the match is.
     * @return true if the provided qualifier is a better match than this one
     */
    @Override
    public boolean isBetterMatchThan(ResourceQualifier compareTo, ResourceQualifier reference) {
        if (compareTo == null) {
            return true;
        }

        KeyboardStateQualifier compareQualifier = (KeyboardStateQualifier) compareTo;
        KeyboardStateQualifier referenceQualifier = (KeyboardStateQualifier) reference;

        // better qualifier
        // only return true if it's a better value.
        return referenceQualifier.mValue == KeyboardState.SOFT &&
                compareQualifier.mValue == KeyboardState.EXPOSED &&
                mValue == KeyboardState.SOFT;
    }

    /**
     * @param qualifier the object to compare
     * @return true if the object is a KeyboardStateQualifier and has the same value as this one.
     */
    @Override
    public boolean equals(Object qualifier) {
        if (qualifier instanceof KeyboardStateQualifier q) {
            return mValue == q.mValue;
        }

        return false;
    }

    /**
     * @return the hash code value for this KeyboardStateQualifier
     */
    @Override
    public int hashCode() {
        return mValue.hashCode();
    }
}
