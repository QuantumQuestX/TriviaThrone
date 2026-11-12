package de.quantumquest.triviathrone.plugin;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class PluginMetadataLoader {
    private static final String DTD_FILE = "plugin.dtd";
    private static final String XSD_FILE = "plugin.xsd";
    private static final String PLUGIN_FILE = "plugin.xml";
    private static final byte[] DOCTYPE_DECLARATION =
            "<!DOCTYPE plugin SYSTEM \"plugin.dtd\">\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] DOCTYPE_MARKER = "<!DOCTYPE".getBytes(StandardCharsets.US_ASCII);
    private static final int MAX_METADATA_SIZE = 1024 * 1024;

    private PluginMetadataLoader() {
    }

    public static PluginDescriptor load(Path pluginJar) throws IOException {
        try (JarFile jar = new JarFile(pluginJar.toFile())) {
            JarEntry entry = jar.getJarEntry(PLUGIN_FILE);
            if (entry == null) {
                throw new IOException("Plugin JAR is missing " + PLUGIN_FILE + ": " + pluginJar);
            }

            try (InputStream input = jar.getInputStream(entry)) {
                return parse(input);
            } catch (ParserConfigurationException | SAXException exception) {
                throw new IOException("Invalid " + PLUGIN_FILE + " in " + pluginJar, exception);
            }
        }
    }

    private static PluginDescriptor parse(InputStream input)
            throws IOException, ParserConfigurationException, SAXException {
        byte[] xml = input.readNBytes(MAX_METADATA_SIZE + 1);
        if (xml.length > MAX_METADATA_SIZE) {
            throw new IOException(PLUGIN_FILE + " exceeds the maximum size of " + MAX_METADATA_SIZE + " bytes");
        }
        xml = addDoctype(xml);

        Schema schema = loadSchema();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(true);
        factory.setSchema(schema);
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
            String resourceName = fileName(systemId);
            if (!DTD_FILE.equals(resourceName)) {
                throw new SAXException("External XML resources are disabled");
            }
            return new InputSource(new ByteArrayInputStream(readResource("/plugin/" + resourceName)));
        });

        Document document = builder.parse(new InputSource(new ByteArrayInputStream(xml)));
        Element root = document.getDocumentElement();

        List<String> providerClasses = new ArrayList<>();
        NodeList providers = root.getElementsByTagName("provider");
        for (int i = 0; i < providers.getLength(); i++) {
            providerClasses.add(((Element) providers.item(i)).getAttribute("class").trim());
        }

        return new PluginDescriptor(
                root.getAttribute("id"),
                text(root, "name"),
                text(root, "version"),
                text(root, "apiVersion"),
                text(root, "description"),
                providerClasses
        );
    }

    private static byte[] addDoctype(byte[] xml) throws SAXException {
        if (contains(xml, DOCTYPE_MARKER)) {
            throw new SAXException(PLUGIN_FILE + " must not declare a DOCTYPE");
        }

        int bomLength = hasUtf8Bom(xml) ? 3 : 0;
        int insertionPoint = bomLength;
        if (startsWith(xml, bomLength, "<?xml")) {
            int declarationEnd = indexOf(xml, "?>".getBytes(StandardCharsets.US_ASCII), bomLength + 5);
            if (declarationEnd < 0) {
                throw new SAXException("Unterminated XML declaration in " + PLUGIN_FILE);
            }
            insertionPoint = declarationEnd + 2;
        }

        byte[] result = new byte[xml.length + DOCTYPE_DECLARATION.length];
        System.arraycopy(xml, 0, result, 0, insertionPoint);
        System.arraycopy(DOCTYPE_DECLARATION, 0, result, insertionPoint, DOCTYPE_DECLARATION.length);
        System.arraycopy(xml, insertionPoint, result, insertionPoint + DOCTYPE_DECLARATION.length,
                xml.length - insertionPoint);
        return result;
    }

    private static boolean contains(byte[] input, byte[] target) {
        return indexOf(input, target, 0) >= 0;
    }

    private static int indexOf(byte[] input, byte[] target, int start) {
        for (int i = start; i <= input.length - target.length; i++) {
            boolean match = true;
            for (int j = 0; j < target.length; j++) {
                if (input[i + j] != target[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }

    private static boolean startsWith(byte[] input, int offset, String value) {
        byte[] expected = value.getBytes(StandardCharsets.US_ASCII);
        if (input.length - offset < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (input[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasUtf8Bom(byte[] input) {
        return input.length >= 3
                && (input[0] & 0xFF) == 0xEF
                && (input[1] & 0xFF) == 0xBB
                && (input[2] & 0xFF) == 0xBF;
    }

    private static Schema loadSchema() throws IOException, SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        var schemaUrl = PluginMetadataLoader.class.getResource("/plugin/" + XSD_FILE);
        if (schemaUrl == null) {
            throw new IOException("Missing bundled schema: " + XSD_FILE);
        }

        try (InputStream input = schemaUrl.openStream()) {
            StreamSource source = new StreamSource(input);
            source.setSystemId(schemaUrl.toExternalForm());
            return factory.newSchema(source);
        }
    }

    private static byte[] readResource(String name) throws IOException {
        try (InputStream input = PluginMetadataLoader.class.getResourceAsStream(name)) {
            if (input == null) {
                throw new IOException("Missing bundled XML resource: " + name);
            }
            return input.readAllBytes();
        }
    }

    private static String text(Element root, String tagName) {
        return root.getElementsByTagName(tagName).item(0).getTextContent().trim();
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
        public void warning(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            throw e;
        }
    }
}
