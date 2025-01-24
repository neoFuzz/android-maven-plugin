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

import com.android.resources.Density;
import com.android.resources.ResourceEnum;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resource Qualifier for Screen Pixel Density.
 */
public final class DensityQualifier extends EnumBasedResourceQualifier {
    /**
     * Pattern to match folder segment of a density qualifier
     */
    public static final String NAME = "Density";
    /**
     * Pattern to match folder segment of a density qualifier
     */
    private static final Pattern sDensityLegacyPattern = Pattern.compile("^(\\d+)dpi$");//$NON-NLS-1$
    /**
     * The enum value
     */
    private Density mValue = Density.MEDIUM;

    /**
     * Constructor
     */
    public DensityQualifier() {
        // pass
    }

    /**
     * @param value Density enum value
     */
    public DensityQualifier(Density value) {
        mValue = value;
    }

    /**
     * @return Density enum value
     */
    public Density getValue() {
        return mValue;
    }

    /**
     * @return enum value
     */
    @Override
    ResourceEnum getEnumValue() {
        return mValue;
    }

    /**
     * @return Name of qualifier
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * @return Short name
     */
    @Override
    public String getShortName() {
        return NAME;
    }

    /**
     * @return 4
     */
    @Override
    public int since() {
        return 4;
    }

    /**
     * @param value  The value to check and set. Must not be null.
     * @param config The folder configuration to receive the value. Must not be null.
     * @return Returns true if the value was accepted
     */
    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        Density density = Density.getEnum(value);
        if (density == null) {

            // attempt to read a legacy value.
            Matcher m = sDensityLegacyPattern.matcher(value);
            if (m.matches()) {
                String v = m.group(1);

                try {
                    density = Density.getEnum(Integer.parseInt(v));
                } catch (NumberFormatException e) {
                    // looks like the string we extracted wasn't a valid number
                    // which really shouldn't happen since the regexp would have failed.
                }
            }
        }

        if (density != null) {
            DensityQualifier qualifier = new DensityQualifier();
            qualifier.mValue = density;
            config.setDensityQualifier(qualifier);
            return true;
        }

        return false;
    }

    @Override
    public boolean isMatchFor(ResourceQualifier qualifier) {
        // as long as there's a density qualifier, it's always a match.
        // The best match will be found later.
        return qualifier instanceof DensityQualifier;
    }

    @Override
    public boolean isBetterMatchThan(ResourceQualifier compareTo, ResourceQualifier reference) {
        if (compareTo == null) {
            return true;
        }

        DensityQualifier compareQ = (DensityQualifier) compareTo;
        DensityQualifier referenceQ = (DensityQualifier) reference;

        if (compareQ.mValue == referenceQ.mValue) {
            // what we have is already the best possible match (exact match)
            return false;
        } else if (mValue == referenceQ.mValue) {
            // got new exact value, this is the best!
            return true;
        } else {
            // in all case we're going to prefer the higher dpi.
            // if reference is high, we want highest dpi.
            // if reference is medium, we'll prefer to scale down high dpi, than scale up low dpi
            // if reference is low, we'll prefer to scale down high than medium (2:1 over 4:3)
            return mValue.getDpiValue() > compareQ.mValue.getDpiValue();
        }
    }

    @Override
    public boolean equals(Object qualifier) {
        return qualifier instanceof DensityQualifier;
    }

    @Override
    public int hashCode() {
        return mValue.hashCode();
    }
}
