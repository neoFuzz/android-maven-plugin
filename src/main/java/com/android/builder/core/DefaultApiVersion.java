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

package com.android.builder.core;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.ApiVersion;

import java.util.Objects;

/**
 * Basic implementation of ApiVersion
 */
public class DefaultApiVersion implements ApiVersion {

    /**
     * The API level
     */
    private final int mApiLevel;

    /**
     * The API codename, or null if none
     */
    @Nullable
    private final String mCodename;

    /**
     * @param apiLevel the API level
     * @param codename the API codename, or null if none
     */
    public DefaultApiVersion(int apiLevel, @Nullable String codename) {
        mApiLevel = apiLevel;
        mCodename = codename;
    }

    /**
     * @param apiLevel the API level
     */
    public DefaultApiVersion(int apiLevel) {
        this(apiLevel, null);
    }

    /**
     * @param codename the API codename
     */
    public DefaultApiVersion(@NonNull String codename) {
        this(1, codename);
    }

    /**
     * @param value the value to create the ApiVersion from
     * @return the ApiVersion
     */
    @NonNull
    public static ApiVersion create(@NonNull Object value) {
        if (value instanceof Integer i) {
            return new DefaultApiVersion(i, null);
        } else if (value instanceof String s) {
            return new DefaultApiVersion(1, s);
        }

        return new DefaultApiVersion(1, null);
    }

    /**
     * @return the API level
     */
    @Override
    public int getApiLevel() {
        return mApiLevel;
    }

    /**
     * @return the API codename, or null if none
     */
    @Nullable
    @Override
    public String getCodename() {
        return mCodename;
    }

    /**
     * @return the API string for this version
     */
    @NonNull
    @Override
    public String getApiString() {
        return mCodename != null ? mCodename : Integer.toString(mApiLevel);
    }

    /**
     * @param o the object to compare to
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultApiVersion that = (DefaultApiVersion) o;

        if (mApiLevel != that.mApiLevel) {
            return false;
        }
        return Objects.equals(mCodename, that.mCodename);
    }

    /**
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        int result = mApiLevel;
        result = 31 * result + (mCodename != null ? mCodename.hashCode() : 0);
        return result;
    }

    /**
     * @return a string suitable for debugging purposes.
     */
    @Override
    public String toString() {
        return "ApiVersionImpl{" +
                "mApiLevel=" + mApiLevel +
                ", mCodename='" + mCodename + '\'' +
                '}';
    }
}
