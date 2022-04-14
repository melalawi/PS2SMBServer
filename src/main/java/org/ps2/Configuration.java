package org.ps2;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;

public class Configuration {
    protected static final String DEFAULT_CONFIG_FILENAME = "fileserver.xml";

    public final String pcROMPath;
    public final String ps2SMBPath;
    public final String ps2ROMArtDirectory;
    public final String ps2ROMLibraryFilePath;

    public Configuration() throws Exception {
        String fileName = System.getProperty("user.dir") + File.separator + DEFAULT_CONFIG_FILENAME;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setExpandEntityReferences(false);
        Document document = factory.newDocumentBuilder().parse(new File(fileName));

        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();

        XPathExpression expr = xpath.compile("//fileserver/ps2/rom/pcpath/text()");
        Node node = (Node) expr.evaluate(document, XPathConstants.NODE);
        pcROMPath = node.getTextContent();

        expr = xpath.compile("//fileserver/ps2/rom/ps2smbpath/text()");
        node = (Node) expr.evaluate(document, XPathConstants.NODE);
        ps2SMBPath = node.getTextContent();

        expr = xpath.compile("//fileserver/ps2/rom/library/text()");
        node = (Node) expr.evaluate(document, XPathConstants.NODE);
        ps2ROMLibraryFilePath = node.getTextContent();

        expr = xpath.compile("//fileserver/ps2/rom/art/text()");
        node = (Node) expr.evaluate(document, XPathConstants.NODE);
        ps2ROMArtDirectory = node.getTextContent();
    }

}
