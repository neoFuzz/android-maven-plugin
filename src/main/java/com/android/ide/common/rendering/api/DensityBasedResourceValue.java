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
package com.android.ide.common.rendering.api;

import com.android.layoutlib.api.IDensityBasedResourceValue;
import com.android.resources.ResourceType;

@SuppressWarnings("deprecation")
public class DensityBasedResourceValue extends ResourceValue implements IDensityBasedResourceValue {
    /**
     * Value for the resource
     */
    private final com.android.resources.Density mDensity;

    /**
     * @param type        The {@link ResourceType} of the resource
     * @param name        The name of the resource
     * @param value       The value of the resource
     * @param density     the density for which this resource is configured.
     * @param isFramework Whether this resource is a framework resource or not
     */
    public DensityBasedResourceValue(ResourceType type, String name, String value,
                                     com.android.resources.Density density, boolean isFramework) {
        super(type, name, value, isFramework);
        mDensity = density;
    }

    /**
     * Returns the density for which this resource is configured.
     *
     * @return the density.
     */
    public com.android.resources.Density getResourceDensity() {
        return mDensity;
    }

    /**
     * Legacy method, do not call
     *
     * @deprecated use {@link #getResourceDensity()} instead.
     */
    @Override
    @Deprecated(since = "4.7")
    public Density getDensity() {
        return Density.getEnum(mDensity.getDpiValue());
    }

    /**
     * @return a string representation of this resource value
     */
    @Override
    public String toString() {
        return "DensityBasedResourceValue ["
                + getResourceType() + "/" + getName() + " = " + getValue()
                + " (density:" + mDensity + ", framework:" + isFramework() + ")]";
    }

    /**
     * @return the hash code of this resource value
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((mDensity == null) ? 0 : mDensity.hashCode());
        return result;
    }

    /**
     * @param obj the object to compare with
     * @return true if the two objects are equal, false otherwise
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        DensityBasedResourceValue other = (DensityBasedResourceValue) obj;
        if (mDensity == null) {
            return other.mDensity == null;
        } else return mDensity.equals(other.mDensity);
    }
}