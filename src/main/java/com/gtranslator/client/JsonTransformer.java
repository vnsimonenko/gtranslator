package com.gtranslator.client;

import com.gtranslator.BaseException;
import com.sun.org.apache.xml.internal.security.utils.Constants;
import net.sf.saxon.jaxp.SaxonTransformerFactory;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class JsonTransformer {
    private Map<String, String> attributes = new HashMap<>();

    private JsonTransformer() {
    }

    public static JsonTransformer createJsonTransformer() {
        return new JsonTransformer();
    }

    public enum XSL {
        HTML("client/translate_html.xsl"), WORDS_HTML("client/translate_words_html.xsl"), TRANSCRIPTION("client/translate_trn.xsl");

        XSL(String fname) {
            this.fname = fname;
        }

        private String fname;
    }

    public JsonTransformer setMaxCount(int count) {
        attributes.put("maxcount", "" + count);
        return this;
    }

    public JsonTransformer setWidth(int width) {
        attributes.put("width", "" + width + "px");
        return this;
    }

    public JsonTransformer setHeight(int height) {
        attributes.put("height", "" + height + "px");
        return this;
    }

    public JsonTransformer setFontFamily(String name) {
        attributes.put("fontfamily", name);
        return this;
    }

    public String convertJsonToHtml(String json, XSL xsl) {
        try {
            System.setProperty("javax.xml.transform.TransformerFactory",
                    "net.sf.saxon.TransformerFactoryImpl");
            SaxonTransformerFactory tfactory = new SaxonTransformerFactory();
            Transformer transformer = tfactory.newTransformer(new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(xsl.fname)));
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "html");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, "text/html");

            String xml = buildXml(json, attributes);
            ByteArrayOutputStream out = new ByteArrayOutputStream(xml.length() * 2);
            transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(out));
            return new String(out.toByteArray(), "UTF-8");
        } catch (TransformerException | UnsupportedEncodingException | ParserConfigurationException ex) {
            throw new BaseException(ex);
        }
    }

    private String buildXml(String json, Map<String, String> attributes) throws ParserConfigurationException, TransformerException, UnsupportedEncodingException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();
        doc.setXmlStandalone(true);
        Element rootElement = doc.createElement("root:document");
        doc.appendChild(rootElement);
        rootElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:root", "http://apache.org/root");
        rootElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:translation", "http://apache.org/translation");
        rootElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:config", "http://apache.org/config");
        Element translateElement = doc.createElement("translation:element");
        translateElement.setTextContent(json);
        for (Map.Entry<String, String> ent : attributes.entrySet()) {
            translateElement.setAttribute(ent.getKey(), ent.getValue());
        }
        rootElement.appendChild(translateElement);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer xmlTransformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(out);
        xmlTransformer.transform(source, result);
        return new String(out.toByteArray(), "UTF-8");
    }
}
