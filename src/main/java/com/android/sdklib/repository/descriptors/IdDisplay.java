/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.sdklib.repository.descriptors;

import com.android.annotations.NonNull;

/**
 * Immutable structure that represents a tuple (id-string  + display-string.)
 */
public final class IdDisplay implements Comparable<IdDisplay> {

    /**
     * The immutable tuple (id-string  + display-string.)
     */
    private final String mId;
    /**
     * The immutable tuple (id-string  + display-string.)
     */
    private final String mDisplay;

    /**
     * Creates a new immutable tuple (id-string  + display-string.)
     *
     * @param id      The non-null id string.
     * @param display The non-null display string.
     */
    public IdDisplay(@NonNull String id, @NonNull String display) {
        mId = id;
        mDisplay = display;
    }

    /**
     * @return The non-null id string.
     */
    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * @return The non-null display string.
     */
    @NonNull
    public String getDisplay() {
        return mDisplay;
    }

    /**
     * {@link IdDisplay} instances are the same if they have the same id.
     * The display value is not used for comparison or ordering.
     *
     * @param tag The {@link IdDisplay} to compare to this one.
     * @return 0 if the ids are the same, -1 or 1 if they are different.
     */
    @Override
    public int compareTo(@NonNull IdDisplay tag) {
        return mId.compareTo(tag.mId);
    }

    /**
     * Hash code of {@link IdDisplay} instances only rely on the id hash code.
     *
     * @return The hash code of the id string.
     */
    @Override
    public int hashCode() {
        return mId.hashCode();
    }

    /**
     * Equality of {@link IdDisplay} instances only rely on the id equality.
     * The display value is not used for comparison or ordering.
     *
     * @param obj The object to compare to this one.
     * @return True if the ids are the same, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof IdDisplay id) && mId.equals(id.mId);
    }

    /**
     * Returns a string representation for *debug* purposes only, not for UI display.
     *
     * @return A string representation of the tuple.
     */
    @Override
    @NonNull
    public String toString() {
        return String.format("%1$s [%2$s]", mId, mDisplay);
    }

}
