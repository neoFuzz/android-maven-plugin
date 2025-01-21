package com.github.cardforge.maven.plugins.android.resource;

import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Combines multiple occurrences of some XML file
 * by appending contents of specified elements.
 */
public class XpathAppendingTransformer implements ResourceTransformer {

    boolean ignoreDtd = true;

    String resource;

    /**
     * XPath expression selecting elements
     */
    String[] elements;

    Document doc;

    public boolean canTransformResource(String r) {
        return resource != null && resource.equalsIgnoreCase(r);
    }

    public void processResource(String resource, InputStream is, List<Relocator> relocatorList) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setExpandEntityReferences(!ignoreDtd);
            DocumentBuilder builder = factory.newDocumentBuilder();

            if (ignoreDtd) {
                builder.setEntityResolver((publicId, systemId) ->
                        new InputSource(new StringReader("")));
            }

            Document newDoc = builder.parse(is);

            if (doc == null) {
                doc = newDoc;
            } else if (elements == null || elements.length == 0) {
                appendElement(newDoc.getDocumentElement(), doc.getDocumentElement());
            } else {
                for (String xpath : elements) {
                    Node source = selectNode(newDoc, xpath);
                    Node target = selectNode(doc, xpath);

                    if (source == null || target == null || source.getNodeType() != Node.ELEMENT_NODE ||
                            target.getNodeType() != Node.ELEMENT_NODE) {
                        throw new IOException("XPath result must be an element: " + xpath);
                    }

                    appendElement((Element) source, (Element) target);
                }
            }
        } catch (Exception e) {
            throw new IOException("Error processing resource " + resource, e);
        }
    }

    @Nullable
    private Node selectNode(Document document, @Nonnull String xpath) {
        // Simple XPath replacement (can use javax.xml.xpath if needed)
        // This is a placeholder implementation; you may need to integrate a full XPath library like XPathFactory
        if (xpath.equals("/")) {
            return document.getDocumentElement();
        }
        return null; // Implement specific XPath parsing logic
    }

    private void appendElement(@Nonnull Element source, Element target) {
        NamedNodeMap attributes = source.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            if (target.getAttributeNode(attribute.getName()) == null) {
                target.setAttributeNode((Attr) target.getOwnerDocument().importNode(attribute, true));
            }
        }

        NodeList children = source.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            target.appendChild(target.getOwnerDocument().importNode(child, true));
        }
    }

    public boolean hasTransformedResource() {
        return doc != null;
    }

    public void modifyOutputStream(@Nonnull JarOutputStream jos) throws IOException {
        jos.putNextEntry(new JarEntry(resource));

        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(jos);

            transformer.transform(source, result);
        } catch (TransformerException e) {
            throw new IOException("Error writing transformed XML to output stream", e);
        }

        doc = null;
    }
}