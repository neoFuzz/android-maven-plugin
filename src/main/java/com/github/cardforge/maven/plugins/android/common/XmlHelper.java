package com.github.cardforge.maven.plugins.android.common;

import com.android.annotations.NonNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Yet another helper class for dealing with XML.
 */
public class XmlHelper {
    private XmlHelper() {
        // private constructor
    }

    public static void removeDirectChildren(@NonNull Node parent) {
        NodeList childNodes = parent.getChildNodes();
        while (childNodes.getLength() > 0) {
            parent.removeChild(childNodes.item(0));
        }
    }

    public static Element getOrCreateElement(Document doc,@NonNull Element manifestElement, String elementName) {
        NodeList nodeList = manifestElement.getElementsByTagName(elementName);
        Element element;
        if (nodeList.getLength() == 0) {
            element = doc.createElement(elementName);
            manifestElement.appendChild(element);
        } else {
            element = (Element) nodeList.item(0);
        }
        return element;
    }
}
