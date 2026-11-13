package quantumquest.triviathrone.auth;

import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

final class ValidatedXmlParser {
    private static final String SCHEMA_INSTANCE_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";

    private ValidatedXmlParser() {
    }

    static Document parse(Path file, String dtdFileName, String xsdFileName)
            throws IOException, SAXException, ParserConfigurationException {
        Schema schema = loadSchema(xsdFileName);
        Document document = parse(file, schema, dtdFileName);
        DocumentType doctype = document.getDoctype();
        String schemaLocation = document.getDocumentElement()
                .getAttributeNS(SCHEMA_INSTANCE_NAMESPACE, "noNamespaceSchemaLocation");

        if (doctype == null || !dtdFileName.equals(fileName(doctype.getSystemId()))
                || !xsdFileName.equals(schemaLocation)) {
            throw new SAXException("XML must reference the configured DTD and XSD");
        }
        return document;
    }

    private static Schema loadSchema(String xsdFileName) throws IOException, SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        ClassPathResource resource = new ClassPathResource("xml/" + xsdFileName);
        try (InputStream input = resource.getInputStream()) {
            StreamSource source = new StreamSource(input);
            source.setSystemId(resource.getURL().toExternalForm());
            return factory.newSchema(source);
        }
    }

    private static Document parse(Path file, Schema schema, String dtdFileName)
            throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setSchema(schema);
        factory.setValidating(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", true);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new StrictErrorHandler());
        builder.setEntityResolver((publicId, systemId) -> {
            if (publicId != null && !publicId.isBlank()) {
                throw new SAXException("Public XML identifiers are disabled");
            }
            return resolveDtd(systemId, dtdFileName);
        });
        try (InputStream input = java.nio.file.Files.newInputStream(file)) {
            return builder.parse(new InputSource(input));
        }
    }

    private static InputSource resolveDtd(String systemId, String expectedDtd) throws SAXException, IOException {
        if (!expectedDtd.equals(fileName(systemId))) {
            throw new SAXException("External XML resources are disabled");
        }
        ClassPathResource resource = new ClassPathResource("xml/" + expectedDtd);
        InputSource source = new InputSource(resource.getInputStream());
        source.setSystemId(resource.getURL().toExternalForm());
        return source;
    }

    private static String fileName(String systemId) {
        if (systemId == null) {
            return "";
        }
        int separator = Math.max(systemId.lastIndexOf('/'), systemId.lastIndexOf('\\'));
        return systemId.substring(separator + 1);
    }

    private static final class StrictErrorHandler implements ErrorHandler {
        @Override
        public void warning(SAXParseException exception) throws SAXException {
            throw exception;
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }
    }
}
