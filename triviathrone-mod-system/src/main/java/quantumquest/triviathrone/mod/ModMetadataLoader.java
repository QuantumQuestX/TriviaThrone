package quantumquest.triviathrone.mod;

import org.w3c.dom.Document;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Reads and securely validates game-mod metadata packaged as {@code mod.xml}.
 *
 * <p>Parsing accepts only the bundled DTD and XSD, rejects caller-supplied document types and
 * public identifiers, and disables external schema access to prevent XML external-entity loading.</p>
 *
 * @author TiJ
 * @since 2.0.0
 */
public final class ModMetadataLoader {
    /**
     * Bundled DTD name used for validation.
     */
    private static final String DTD_FILE = "mod.dtd";
    /**
     * Bundled XML Schema name used for validation.
     */
    private static final String XSD_FILE = "mod.xsd";
    /**
     * Required mod metadata entry name within a JAR.
     */
    private static final String MOD_FILE = "mod.xml";
    /**
     * ASCII marker used to reject a mod-supplied document type declaration.
     */
    private static final byte[] DOCTYPE_MARKER = "<!DOCTYPE".getBytes(StandardCharsets.US_ASCII);
    /**
     * Maximum accepted metadata size, limiting parser resource consumption.
     */
    private static final int MAX_METADATA_SIZE = 1024 * 1024;

    /**
     * Prevents instantiation of this utility class.
     */
    private ModMetadataLoader() {
    }

    /**
     * Loads and validates metadata from a game-mod JAR.
     *
     * @param modJar JAR containing {@value #MOD_FILE}
     * @return validated mod metadata
     * @throws IOException if the JAR, metadata, or validation resources cannot be read or parsed
     */
    public static ModDescriptor load(Path modJar) throws IOException {
        try (JarFile jar = new JarFile(modJar.toFile())) {
            JarEntry entry = jar.getJarEntry(MOD_FILE);
            if (entry == null) {
                throw new IOException("Game mod JAR is missing " + MOD_FILE + ": " + modJar);
            }
            try (InputStream input = jar.getInputStream(entry)) {
                return parse(input);
            } catch (ParserConfigurationException | SAXException exception) {
                throw new IOException("Invalid " + entry.getName() + " in " + modJar, exception);
            }
        }
    }

    /**
     * Parses bounded metadata bytes using the bundled, security-restricted validation resources.
     *
     * @param input stream for the metadata entry
     * @return parsed metadata
     * @throws IOException                  if the metadata exceeds its size limit or a resource cannot be read
     * @throws ParserConfigurationException if a secure parser cannot be configured
     * @throws SAXException                 if metadata violates XML, DTD, or schema constraints
     */
    private static ModDescriptor parse(InputStream input)
        throws IOException, ParserConfigurationException, SAXException {
        byte[] xml = input.readNBytes(MAX_METADATA_SIZE + 1);
        if (xml.length > MAX_METADATA_SIZE) {
            throw new IOException(MOD_FILE + " exceeds the maximum size of " + MAX_METADATA_SIZE + " bytes");
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
            return new InputSource(new ByteArrayInputStream(readResource("/mod/" + resourceName)));
        });

        Document document = builder.parse(new InputSource(new ByteArrayInputStream(xml)));
        Element root = document.getDocumentElement();
        if (!"mod".equals(root.getTagName())) {
            throw new SAXException("The root element in " + MOD_FILE + " must be <mod>.");
        }

        List<String> providerClasses = new ArrayList<>();
        NodeList providers = root.getElementsByTagName("provider");
        for (int i = 0; i < providers.getLength(); i++) {
            providerClasses.add(((Element) providers.item(i)).getAttribute("class").trim());
        }

        return new ModDescriptor(
            root.getAttribute("id"),
            text(root, "name"),
            text(root, "version"),
            text(root, "apiVersion"),
            text(root, "description"),
            providerClasses
        );
    }

    /**
     * Adds the sole allowed local DTD declaration after rejecting any supplied document type.
     *
     * @param xml metadata bytes, optionally with a UTF-8 BOM and XML declaration
     * @return metadata bytes with the bundled DTD declaration inserted
     * @throws SAXException if supplied metadata declares a document type or has an unterminated XML declaration
     */
    private static byte[] addDoctype(byte[] xml) throws SAXException {
        if (contains(xml, DOCTYPE_MARKER)) {
            throw new SAXException("Mod metadata must not declare a DOCTYPE");
        }
        byte[] doctypeDeclaration = ("<!DOCTYPE mod SYSTEM \"" + DTD_FILE + "\">\n")
            .getBytes(StandardCharsets.US_ASCII);

        int bomLength = hasUtf8Bom(xml) ? 3 : 0;
        int insertionPoint = bomLength;
        if (startsWith(xml, bomLength, "<?xml")) {
            int declarationEnd = indexOf(xml, "?>".getBytes(StandardCharsets.US_ASCII), bomLength + 5);
            if (declarationEnd < 0) {
                throw new SAXException("Unterminated XML declaration in mod metadata");
            }
            insertionPoint = declarationEnd + 2;
        }

        byte[] result = new byte[xml.length + doctypeDeclaration.length];
        System.arraycopy(xml, 0, result, 0, insertionPoint);
        System.arraycopy(doctypeDeclaration, 0, result, insertionPoint, doctypeDeclaration.length);
        System.arraycopy(xml, insertionPoint, result, insertionPoint + doctypeDeclaration.length,
            xml.length - insertionPoint);
        return result;
    }

    /**
     * Determines whether a byte sequence occurs within another sequence.
     *
     * @param input  bytes to search
     * @param target bytes to find
     * @return whether {@code target} occurs in {@code input}
     */
    private static boolean contains(byte[] input, byte[] target) {
        return indexOf(input, target, 0) >= 0;
    }

    /**
     * Finds the first occurrence of a byte sequence at or after an offset.
     *
     * @param input  bytes to search
     * @param target bytes to find
     * @param start  inclusive search offset
     * @return matching offset, or {@code -1}
     */
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

    /**
     * Tests whether bytes at an offset begin with an ASCII string.
     *
     * @param input  bytes to inspect
     * @param offset start offset
     * @param value  expected ASCII value
     * @return whether the expected value begins at the offset
     */
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

    /**
     * Tests whether input begins with a UTF-8 byte-order mark.
     *
     * @param input bytes to inspect
     * @return whether a UTF-8 BOM is present
     */
    private static boolean hasUtf8Bom(byte[] input) {
        return input.length >= 3
            && (input[0] & 0xFF) == 0xEF
            && (input[1] & 0xFF) == 0xBB
            && (input[2] & 0xFF) == 0xBF;
    }

    /**
     * Loads the bundled schema with all external resource access disabled.
     *
     * @return compiled bundled schema
     * @throws IOException  if the schema resource is missing or unreadable
     * @throws SAXException if the bundled schema is invalid
     */
    private static Schema loadSchema() throws IOException, SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        var schemaUrl = ModMetadataLoader.class.getResource("/mod/" + XSD_FILE);
        if (schemaUrl == null) {
            throw new IOException("Missing bundled schema: " + XSD_FILE);
        }

        try (InputStream input = schemaUrl.openStream()) {
            StreamSource source = new StreamSource(input);
            source.setSystemId(schemaUrl.toExternalForm());
            return factory.newSchema(source);
        }
    }

    /**
     * Reads a required bundled XML resource.
     *
     * @param name class-path resource name
     * @return complete resource contents
     * @throws IOException if the resource is missing or cannot be read
     */
    private static byte[] readResource(String name) throws IOException {
        try (InputStream input = ModMetadataLoader.class.getResourceAsStream(name)) {
            if (input == null) {
                throw new IOException("Missing bundled XML resource: " + name);
            }
            return input.readAllBytes();
        }
    }

    /**
     * Obtains trimmed text from a required child element.
     *
     * @param root    metadata root element
     * @param tagName child element name
     * @return trimmed child text
     */
    private static String text(Element root, String tagName) {
        return root.getElementsByTagName(tagName).item(0).getTextContent().trim();
    }

    /**
     * Extracts a resource file name from a slash- or backslash-separated system identifier.
     *
     * @param systemId XML system identifier, possibly {@code null}
     * @return final path component, or an empty string
     */
    private static String fileName(String systemId) {
        if (systemId == null) {
            return "";
        }
        int separator = Math.max(systemId.lastIndexOf('/'), systemId.lastIndexOf('\\'));
        return systemId.substring(separator + 1);
    }

    /**
     * Converts all parser diagnostics into fatal parse failures.
     *
     * @author TiJ
     * @since 2.0.0
     */
    private static final class StrictErrorHandler implements ErrorHandler {
        /**
         * Rejects a parser warning.
         *
         * @param e reported warning
         * @throws SAXException always
         */
        @Override
        public void warning(SAXParseException e) throws SAXException {
            throw e;
        }

        /**
         * Rejects a parser error.
         *
         * @param e reported error
         * @throws SAXException always
         */
        @Override
        public void error(SAXParseException e) throws SAXException {
            throw e;
        }

        /**
         * Propagates a fatal parser error.
         *
         * @param e reported fatal error
         * @throws SAXException always
         */
        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            throw e;
        }
    }
}
