/*
 * Copyright (C) 2012 The Android Open Source Project
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
import com.android.io.FileWrapper;
import com.android.io.IAbstractFile;
import com.android.io.StreamException;
import com.android.utils.XmlUtils;
import com.android.xml.AndroidXPathFactory;
import com.google.common.io.Closeables;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Default implementation of {@link ManifestParser}
 */
public class DefaultManifestParser implements ManifestParser {

    /**
     * The minimum SDK version
     */
    Optional<Object> mMinSdkVersion;
    /**
     * The target SDK version
     */
    Optional<Object> mTargetSdkVersion;
    /**
     * The version code
     */
    Optional<Integer> mVersionCode;
    /**
     * tha package
     */
    Optional<String> mPackage;
    /**
     * The version name
     */
    Optional<String> mVersionName;

    /**
     * @param file  the manifest file to parse
     * @param xPath the xpath expression to evaluate
     * @return the value of the xpath expression, or null if not found
     */
    @Nullable
    private static String getStringValue(@NonNull File file, @NonNull String xPath) {
        XPath xpath = AndroidXPathFactory.newXPath();

        try {
            InputSource source = new InputSource(XmlUtils.getUtfReader(file));
            return xpath.evaluate(xPath, source);
        } catch (XPathExpressionException e) {
            // won't happen.
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    /**
     * @param manifestFile the manifest file to parse
     * @return the target SDK version, or null if not found
     * @throws StreamException if an error occurs while reading the manifest file
     */
    @Nullable
    public static Object getTargetSdkVersionAM(IAbstractFile manifestFile) throws StreamException {
        String result = getStringValueAM(manifestFile);

        try {
            return Integer.valueOf(result);
        } catch (NumberFormatException var3) {
            return !result.isEmpty() ? result : null;
        }
    }

    /**
     * @param file the manifest file to parse
     * @return the target SDK version, or null if not found
     * @throws StreamException if an error occurs while reading the manifest file
     */
    private static String getStringValueAM(@NonNull IAbstractFile file) throws StreamException {
        String strXPath = "/manifest/uses-sdk/@android:targetSdkVersion"; // NOSONAR
        XPath xpath = AndroidXPathFactory.newXPath();
        InputStream is = null;

        String e;
        try {
            is = file.getContents();
            e = xpath.evaluate(strXPath, new InputSource(is));
        } catch (XPathExpressionException ex) {
            throw new RuntimeException(
                    "Malformed XPath expression when reading the attribute from the manifest,exp = " + strXPath, ex);
        } finally {
            Closeables.closeQuietly(is);
        }

        return e;
    }

    /**
     * @param manifestFile the manifest file to parse
     * @return the min SDK version, or null if not found
     */
    @Nullable
    public static Object getMinSdkVersionAM(IAbstractFile manifestFile) {
        String result = getStringValue((File) manifestFile, "/manifest/uses-sdk/@android:minSdkVersion");

        try {
            return Integer.valueOf(result);
        } catch (NumberFormatException ignored) {
            return !result.isEmpty() ? result : null;
        }
    }

    /**
     * @param manifestFile the manifest file to parse
     * @return the package name, or null if not found
     */
    @Nullable
    @Override
    public synchronized String getPackage(@NonNull File manifestFile) {
        if (mPackage == null) {
            mPackage = Optional.ofNullable(getStringValue(manifestFile, "/manifest/@package"));
        }
        return mPackage.orElse(null);
    }

    /**
     * @param manifestFile the manifest file to parse
     * @return the version name, or null if not found
     */
    @Nullable
    @Override
    public synchronized String getVersionName(@NonNull File manifestFile) {
        if (mVersionName == null) {
            mVersionName = Optional.ofNullable(
                    getStringValue(manifestFile, "/manifest/@android:versionName"));
        }
        return mVersionName.orElse(null);
    }

    /**
     * @param manifestFile the manifest file to parse
     * @return the version code, or -1 if not found
     */
    @Override
    public synchronized int getVersionCode(@NonNull File manifestFile) {
        if (mVersionCode == null) {
            mVersionCode = Optional.empty();
            try {
                String value = getStringValue(manifestFile, "/manifest/@android:versionCode");
                if (value != null) {
                    mVersionCode = Optional.of(Integer.valueOf(value));
                }
            } catch (NumberFormatException ignored) {
                // ignored
            }
        }
        return mVersionCode.orElse(-1);
    }

    /**
     * @param manifestFile the manifest file to parse
     * @return the min SDK version, or 1 if not found
     */
    @Override
    @NonNull
    public synchronized Object getMinSdkVersion(@NonNull File manifestFile) {
        if (mMinSdkVersion == null) {
            try {
                mMinSdkVersion = Optional.ofNullable(
                        getMinSdkVersionAM(new FileWrapper(manifestFile)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return mMinSdkVersion.orElse(1);
    }

    /**
     * @param manifestFile the manifest file to parse
     * @return the target SDK version, or -1 if not found
     */
    @Override
    @NonNull
    public Object getTargetSdkVersion(@NonNull File manifestFile) {
        if (mTargetSdkVersion == null) {
            try {
                mTargetSdkVersion =
                        Optional.ofNullable(getTargetSdkVersionAM(
                                new FileWrapper(manifestFile)));
            } catch (StreamException e) {
                throw new RuntimeException(e);
            }
        }
        return mTargetSdkVersion.orElse(-1);
    }

}
