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

package com.android.ide.common.resources;

import com.android.ide.common.rendering.api.*;
import com.android.ide.common.res2.ValueXmlHelper;
import com.android.resources.ResourceType;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX handler to parse value resource files.
 */
public final class ValueResourceParser extends DefaultHandler {
    /**
     * The <code>resources</code> node
     */
    private static final String NODE_RESOURCES = "resources";
    /**
     * The <code>item</code> node
     */
    private static final String NODE_ITEM = "item";
    /**
     * The attribute name
     */
    private static final String ATTR_NAME = "name";
    /**
     * The attribute type
     */
    private static final String ATTR_TYPE = "type";
    /**
     * The attribute parent
     */
    private static final String ATTR_PARENT = "parent";
    /**
     * The attribute value
     */
    private static final String ATTR_VALUE = "value";

    /**
     * The default namespace prefix
     */
    private static final String DEFAULT_NS_PREFIX = "android:";
    /**
     * The default namespace prefix length
     */
    private static final int DEFAULT_NS_PREFIX_LEN = DEFAULT_NS_PREFIX.length();
    /**
     * Whether the repository is in the framework namespace
     */
    private final boolean mIsFramework;
    /**
     * The repository to store the parsed values in
     */
    private final IValueResourceRepository mRepository;
    /**
     * Whether we are in the <code>resources</code> node
     */
    private boolean inResources = false;
    /**
     * The current resource value
     */
    private int mDepth = 0;
    /**
     * The current resource value
     */
    private ResourceValue mCurrentValue = null;
    /**
     * The current style value
     */
    private StyleResourceValue mCurrentStyle = null;
    /**
     * The current declare-styleable value
     */
    private DeclareStyleableResourceValue mCurrentDeclareStyleable = null;
    /**
     * The current attribute value
     */
    private AttrResourceValue mCurrentAttr;

    /**
     * @param repository  The repository to store the parsed values in
     * @param isFramework Whether the repository is in the framework namespace
     */
    public ValueResourceParser(IValueResourceRepository repository, boolean isFramework) {
        mRepository = repository;
        mIsFramework = isFramework;
    }

    /**
     * @param uri       The Namespace URI, or the empty string if the
     *                  element has no Namespace URI or if Namespace
     *                  processing is not being performed.
     * @param localName The local name (without prefix), or the
     *                  empty string if Namespace processing is not being
     *                  performed.
     * @param qName     The qualified name (with prefix), or the
     *                  empty string if qualified names are not available.
     * @throws SAXException Any SAX exception, possibly wrapping another exception.
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (mCurrentValue != null) {
            String value = mCurrentValue.getValue();
            value = value == null ? "" : ValueXmlHelper.unescapeResourceString(value, false, true);
            mCurrentValue.setValue(value);
        }

        if (inResources && qName.equals(NODE_RESOURCES)) {
            inResources = false;
        } else if (mDepth == 2) {
            mCurrentValue = null;
            mCurrentStyle = null;
            mCurrentDeclareStyleable = null;
            mCurrentAttr = null;
        } else if (mDepth == 3) {
            mCurrentValue = null;
            if (mCurrentDeclareStyleable != null) {
                mCurrentAttr = null;
            }
        }

        mDepth--;
        super.endElement(uri, localName, qName);
    }

    /**
     * @param uri        The Namespace URI, or the empty string if the
     *                   element has no Namespace URI or if Namespace
     *                   processing is not being performed.
     * @param localName  The local name (without prefix), or the
     *                   empty string if Namespace processing is not being
     *                   performed.
     * @param qName      The qualified name (with prefix), or the
     *                   empty string if qualified names are not available.
     * @param attributes The attributes attached to the element.  If
     *                   there are no attributes, it shall be an empty
     *                   Attributes object.
     * @throws SAXException Any SAX exception, possibly wrapping another exception.
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        try {
            mDepth++;
            if (!inResources && mDepth == 1) {
                if (qName.equals(NODE_RESOURCES)) {
                    inResources = true;
                }
            } else if (mDepth == 2 && inResources) {
                ResourceType type = getType(qName, attributes);

                if (type != null) {
                    // get the resource name
                    String name = attributes.getValue(ATTR_NAME);
                    if (name != null) {
                        switch (type) {
                            case STYLE:
                                String parent = attributes.getValue(ATTR_PARENT);
                                mCurrentStyle = new StyleResourceValue(type, name, parent,
                                        mIsFramework);
                                mRepository.addResourceValue(mCurrentStyle);
                                break;
                            case DECLARE_STYLEABLE:
                                mCurrentDeclareStyleable = new DeclareStyleableResourceValue(
                                        type, name, mIsFramework);
                                mRepository.addResourceValue(mCurrentDeclareStyleable);
                                break;
                            case ATTR:
                                mCurrentAttr = new AttrResourceValue(type, name, mIsFramework);
                                mRepository.addResourceValue(mCurrentAttr);
                                break;
                            default:
                                mCurrentValue = new ResourceValue(type, name, mIsFramework);
                                mRepository.addResourceValue(mCurrentValue);
                                break;
                        }
                    }
                }
            } else if (mDepth == 3) {
                // get the resource name
                String name = attributes.getValue(ATTR_NAME);
                if (name != null) {

                    if (mCurrentStyle != null) {
                        // is the attribute in the android namespace?
                        boolean isFrameworkAttr = mIsFramework;
                        if (name.startsWith(DEFAULT_NS_PREFIX)) {
                            name = name.substring(DEFAULT_NS_PREFIX_LEN);
                            isFrameworkAttr = true;
                        }

                        mCurrentValue = new ItemResourceValue(name, isFrameworkAttr, mIsFramework);
                        mCurrentStyle.addItem((ItemResourceValue) mCurrentValue);
                    } else if (mCurrentDeclareStyleable != null) {
                        // is the attribute in the android namespace?
                        boolean isFramework = mIsFramework;
                        if (name.startsWith(DEFAULT_NS_PREFIX)) {
                            name = name.substring(DEFAULT_NS_PREFIX_LEN);
                            isFramework = true;
                        }

                        mCurrentAttr = new AttrResourceValue(ResourceType.ATTR, name, isFramework);
                        mCurrentDeclareStyleable.addValue(mCurrentAttr);

                        // also add it to the repository.
                        mRepository.addResourceValue(mCurrentAttr);

                    } else if (mCurrentAttr != null) {
                        // get the enum/flag value
                        String value = attributes.getValue(ATTR_VALUE);

                        try {
                            // Integer.decode/parseInt can't deal with hex value > 0x7FFFFFFF so we
                            // use Long.decode instead.
                            mCurrentAttr.addValue(name, (int) (long) Long.decode(value));
                        } catch (NumberFormatException e) {
                            // pass, we'll just ignore this value
                        }

                    }
                }
            } else if (mDepth == 4 && mCurrentAttr != null) {
                // get the enum/flag name
                String name = attributes.getValue(ATTR_NAME);
                String value = attributes.getValue(ATTR_VALUE);

                try {
                    // Integer.decode/parseInt can't deal with hex value > 0x7FFFFFFF so we
                    // use Long.decode instead.
                    mCurrentAttr.addValue(name, (int) (long) Long.decode(value));
                } catch (NumberFormatException e) {
                    // pass, we'll just ignore this value
                }
            }
        } finally {
            super.startElement(uri, localName, qName, attributes);
        }
    }

    /**
     * @param qName      The qualified name (with prefix, if any).
     * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
     * @return the {@link ResourceType} or null if the type is not supported.
     */
    private ResourceType getType(String qName, Attributes attributes) {
        String typeValue;

        // if the node is <item>, we get the type from the attribute "type"
        if (NODE_ITEM.equals(qName)) {
            typeValue = attributes.getValue(ATTR_TYPE);
        } else {
            // the type is the name of the node.
            typeValue = qName;
        }

        return ResourceType.getEnum(typeValue);
    }

    /**
     * @param ch     The characters.
     * @param start  The start position in the character array.
     * @param length The number of characters to use from the
     *               character array.
     * @throws SAXException Any SAX exception, possibly wrapping another exception.
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (mCurrentValue != null) {
            String value = mCurrentValue.getValue();
            if (value == null) {
                mCurrentValue.setValue(new String(ch, start, length));
            } else {
                mCurrentValue.setValue(value + new String(ch, start, length));
            }
        }
    }


    /**
     * Interface implemented by an object that can receive resource values as they are parsed from
     * XML.
     */
    public interface IValueResourceRepository {
        /**
         * @param value the {@link ResourceValue} to add to the repository
         */
        void addResourceValue(ResourceValue value);

        /**
         * @param type the type of the resource to look for
         * @param name the name of the resource to look for
         * @return true if the repository contains a resource of the given type and name
         */
        boolean hasResourceValue(ResourceType type, String name);
    }


}
