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

package com.android.manifmerger;

import com.android.annotations.NonNull;
import com.android.ide.common.blame.SourceFile;
import com.android.utils.PositionXmlParser;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static com.android.manifmerger.PlaceholderHandler.KeyBasedValueResolver;

/**
 * Responsible for loading XML files.
 */
public final class XmlLoader {

    private XmlLoader() {
    }

    /**
     * Loads an XML file without doing xml validation and return a {@link XmlDocument}
     *
     * @param displayName             the xml file display name.
     * @param xmlFile                 the xml file.
     * @param selectors               the key resolver to use to resolve selectors.
     * @param systemPropertyResolver  the key resolver to use to resolve system properties.
     * @param type                    the file type.
     * @param mainManifestPackageName the package name of the main manifest, if any.
     * @param inputStream             the input stream to read the xml file from.
     * @return the initialized {@link com.android.manifmerger.XmlDocument}
     * @throws IOException                  if the file cannot be read.
     * @throws SAXException                 if the xml is incorrect
     * @throws ParserConfigurationException if the xml engine cannot be configured.
     */
    @NonNull
    public static XmlDocument load(
            @NonNull KeyResolver<String> selectors,
            @NonNull KeyBasedValueResolver<ManifestSystemProperty> systemPropertyResolver,
            @NonNull String displayName,
            @NonNull File xmlFile,
            @NonNull InputStream inputStream,
            @NonNull XmlDocument.Type type,
            @NonNull Optional<String> mainManifestPackageName)
            throws IOException, SAXException, ParserConfigurationException {
        Document domDocument = PositionXmlParser.parse(inputStream);
        return new XmlDocument(
                new SourceFile(xmlFile, displayName),
                selectors,
                systemPropertyResolver,
                domDocument.getDocumentElement(),
                type,
                mainManifestPackageName);
    }

    /**
     * Loads an XML document from its {@link String} representation without doing xml validation and
     * return a {@link com.android.manifmerger.XmlDocument}
     *
     * @param sourceFile              the source location to use for logging and record collection.
     * @param xml                     the persisted xml.
     * @param type                    the file type.
     * @param mainManifestPackageName the package name of the main manifest, if any.
     * @param selectors               the key resolver to use to resolve selectors.
     * @param systemPropertyResolver  the key resolver to use to resolve system properties.
     * @return the initialized {@link com.android.manifmerger.XmlDocument}
     * @throws IOException                  this should never be thrown.
     * @throws SAXException                 if the xml is incorrect
     * @throws ParserConfigurationException if the xml engine cannot be configured.
     */
    @NonNull
    public static XmlDocument load(
            @NonNull KeyResolver<String> selectors,
            @NonNull KeyBasedValueResolver<ManifestSystemProperty> systemPropertyResolver,
            @NonNull SourceFile sourceFile,
            @NonNull String xml,
            @NonNull XmlDocument.Type type,
            @NonNull Optional<String> mainManifestPackageName)
            throws IOException, SAXException, ParserConfigurationException {
        Document domDocument = PositionXmlParser.parse(xml);
        return new XmlDocument(
                sourceFile,
                selectors,
                systemPropertyResolver,
                domDocument.getDocumentElement(),
                type,
                mainManifestPackageName);
    }
}
