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
package com.android.ide.common.rendering.api;

import com.android.annotations.NonNull;

/**
 * Represents each item in the android style resource.
 */
public class ItemResourceValue extends ResourceValue {
    private final boolean mIsFrameworkAttr;

    /**
     * @param name             the name of the resource.
     * @param isFrameworkAttr  if the attribute is in framework namespace.
     * @param isFrameworkStyle if the style is a framework file or project file.
     * @see #ItemResourceValue(String, boolean, String, boolean)
     */
    public ItemResourceValue(String name, boolean isFrameworkAttr, boolean isFrameworkStyle) {
        this(name, isFrameworkAttr, null, isFrameworkStyle);
    }

    /**
     * If the value is a reference to a framework resource or not is NOT represented with a boolean!
     * but can be deduced with:
     * <pre> {@code
     *        boolean isFrameworkValue = item.isFramework() ||
     *            item.getValue().startsWith(SdkConstants.ANDROID_PREFIX) ||
     *            item.getValue().startsWith(SdkConstants.ANDROID_THEME_PREFIX);
     * } </pre>
     * For {@code <item name="foo">bar</item>}, item in a style resource, the values of the
     * parameters will be as follows:
     *
     * @param attributeName    foo
     * @param isFrameworkAttr  is foo in framework namespace.
     * @param value            bar (in case of a reference, the value may include the namespace.
     *                         if the namespace is absent, default namespace is assumed based on
     *                         isFrameworkStyle (android namespace when isFrameworkStyle=true and app
     *                         namespace when isFrameworkStyle=false))
     * @param isFrameworkStyle if the style is a framework file or project file.
     */
    public ItemResourceValue(String attributeName, boolean isFrameworkAttr, String value,
                             boolean isFrameworkStyle) {
        super(null, attributeName, value, isFrameworkStyle);
        mIsFrameworkAttr = isFrameworkAttr;
    }

    /**
     * @param res             the resource value
     * @param isFrameworkAttr is it in the framework namespace
     * @return the item resource value
     */
    @NonNull
    static ItemResourceValue fromResourceValue(@NonNull ResourceValue res, boolean isFrameworkAttr) {
        assert res.getResourceType() == null : res.getResourceType() + " is not null";
        return new ItemResourceValue(res.getName(), isFrameworkAttr, res.getValue(),
                res.isFramework());
    }

    /**
     * @return if the attribute is in framework namespace.
     */
    public boolean isFrameworkAttr() {
        return mIsFrameworkAttr;
    }

    /**
     * @return the attribute name and if it is in framework namespace
     */
    Attribute getAttribute() {
        return new Attribute(getName(), mIsFrameworkAttr);
    }

    /**
     * Represents an attribute name and if it is in framework namespace.
     */
    static final class Attribute {
        /**
         * The name of the attribute. This is the same as the name in the XML file.
         */
        String mName;
        /**
         * If the attribute is in framework namespace.
         */
        boolean mIsFrameworkAttr;

        /**
         * @param name            the name of the attribute. This is the same as the name in the XML file.
         * @param isFrameworkAttr if the attribute is in framework namespace.
         * @see #Attribute(String, boolean)
         */
        Attribute(String name, boolean isFrameworkAttr) {
            mName = name;
            mIsFrameworkAttr = isFrameworkAttr;
        }

        /**
         * @return a hash code for this object
         */
        @Override
        public int hashCode() {
            int booleanHash = mIsFrameworkAttr ? 1231 : 1237;  // see java.lang.Boolean#hashCode()
            return 31 * booleanHash + mName.hashCode();
        }

        /**
         * @param obj the object to compare with
         * @return true if the objects are equal, false otherwise
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Attribute) {
                Attribute attr = (Attribute) obj;
                return mIsFrameworkAttr == attr.mIsFrameworkAttr && mName.equals(attr.mName);
            }
            return false;
        }

        /**
         * @return a string representation of this object
         */
        @Override
        @NonNull
        public String toString() {
            return mName + " (framework:" + mIsFrameworkAttr + ")";
        }
    }
}