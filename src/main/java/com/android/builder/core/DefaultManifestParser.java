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
import com.android.utils.XmlUtils;
import com.android.xml.AndroidXPathFactory;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Default implementation
 */
public class DefaultManifestParser {

    /**
     * tha package
     */
    Optional<String> mPackage;

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
     * @return the package name, or null if not found
     */
    @Nullable
    public synchronized String getPackage(@NonNull File manifestFile) {
        if (mPackage == null) {
            mPackage = Optional.ofNullable(getStringValue(manifestFile, "/manifest/@package"));
        }
        return mPackage.orElse(null);
    }

}
