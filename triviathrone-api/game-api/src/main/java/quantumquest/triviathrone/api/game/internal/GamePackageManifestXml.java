package quantumquest.triviathrone.api.game.internal;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import quantumquest.triviathrone.api.game.GamePackageManifest;
import quantumquest.triviathrone.api.game.GamePackageManifest.Asset;
import quantumquest.triviathrone.api.game.GamePackageManifest.PackageMetadata;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Serializes and parses the XML representation of game package manifests.
 *
 * <p>This internal utility is stateless. Parsing disables external entities and validates package
 * files before returning a manifest.</p>
 *
 * @author TiJ
 * @since 2.0.0
 */
public final class GamePackageManifestXml {
    /**
     * Legacy manifest format version recognized by the XML parser.
     */
    private static final String LEGACY_MANIFEST_VERSION = "1.0";
    /**
     * Maximum accepted manifest size in bytes.
     */
    private static final int MAX_MANIFEST_SIZE = 1024 * 1024;

    /**
     * Prevents instantiation of this utility class.
     */
    private GamePackageManifestXml() {
    }

    /**
     * Reads an XML manifest from a package root and validates its declared files.
     *
     * @param packageRoot non-null package root
     * @return a non-null validated manifest
     * @throws IOException              if the manifest or its declared files cannot be read
     * @throws IllegalArgumentException if the manifest is malformed, unsafe, or invalid
     */
    public static GamePackageManifest read(Path packageRoot) throws IOException {
        Objects.requireNonNull(packageRoot, "packageRoot");
        Path packageDirectory = packageRoot.toAbsolutePath().normalize();
        Path manifestFile = packageDirectory.resolve(GamePackageManifest.FILE_NAME);
        if (!Files.isRegularFile(manifestFile, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException(GamePackageManifest.FILE_NAME + " is required in a game package.");
        }

        Path realRoot = packageDirectory.toRealPath();
        if (!manifestFile.toRealPath().startsWith(realRoot)) {
            throw new IllegalArgumentException(GamePackageManifest.FILE_NAME + " resolves outside the game package.");
        }

        byte[] xml;
        try (InputStream input = Files.newInputStream(manifestFile)) {
            xml = input.readNBytes(MAX_MANIFEST_SIZE + 1);
        }
        if (xml.length > MAX_MANIFEST_SIZE) {
            throw new IllegalArgumentException(GamePackageManifest.FILE_NAME + " exceeds the maximum size.");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
            Element root = document.getDocumentElement();
            if (!"gamePackage".equals(root.getTagName())) {
                throw new IllegalArgumentException("The package manifest root must be <gamePackage>.");
            }

            String manifestVersion = requiredAttribute(root, "manifestVersion");
            Element mod;
            if (GamePackageManifest.CURRENT_MANIFEST_VERSION.equals(manifestVersion)) {
                mod = requiredChild(root, "mod");
            } else if (LEGACY_MANIFEST_VERSION.equals(manifestVersion)) {
                mod = requiredChild(root, "plugin");
            } else {
                throw new IllegalArgumentException("Unsupported manifest version: " + manifestVersion);
            }

            Element game = requiredChild(root, "game");
            Element createdBy = optionalChild(root, "createdBy");
            Element metadataElement = optionalChild(root, "metadata");
            Element assetsElement = optionalChild(root, "assets");

            List<Asset> assets = readAssets(assetsElement);
            PackageMetadata metadata = readMetadata(metadataElement);
            GamePackageManifest manifest = new GamePackageManifest(
                    manifestVersion,
                    requiredAttribute(mod, "id"),
                    requiredAttribute(mod, "version"),
                    requiredAttribute(mod, "apiVersion"),
                    requiredAttribute(game, "type"),
                    requiredAttribute(game, "providerClass"),
                    requiredAttribute(game, "dataFile"),
                    requiredAttribute(game, "formatVersion"),
                    createdBy == null ? null : optionalAttribute(createdBy, "serverVersion"),
                    metadata,
                    assets
            );
            manifest.validateFiles(packageRoot);

            return manifest;
        } catch (ParserConfigurationException | SAXException exception) {
            throw new IllegalArgumentException("Could not parse " + GamePackageManifest.FILE_NAME + ".", exception);
        }
    }

    /**
     * Writes a manifest in XML format without closing the supplied stream.
     *
     * @param manifest non-null manifest to serialize
     * @param output   non-null destination stream owned by the caller
     * @throws IOException if XML serialization fails
     */
    public static void write(GamePackageManifest manifest, OutputStream output) throws IOException {
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(output, "output");

        try {
            XMLStreamWriter xml = XMLOutputFactory.newFactory().createXMLStreamWriter(output, "UTF-8");
            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeStartElement("gamePackage");
            xml.writeAttribute("manifestVersion", manifest.manifestVersion());
            xml.writeEmptyElement(
                    GamePackageManifest.CURRENT_MANIFEST_VERSION.equals(manifest.manifestVersion()) ? "mod" : "plugin"
            );
            xml.writeAttribute("id", manifest.modId());
            xml.writeAttribute("version", manifest.modVersion());
            xml.writeAttribute("apiVersion", manifest.apiVersion());
            xml.writeEmptyElement("game");
            xml.writeAttribute("type", manifest.gameType());
            xml.writeAttribute("providerClass", manifest.providerClass());
            xml.writeAttribute("dataFile", manifest.gameDataFile());
            xml.writeAttribute("formatVersion", manifest.gameDataFormatVersion());

            if (manifest.serverVersion() != null) {
                xml.writeEmptyElement("createdBy");
                xml.writeAttribute("serverVersion", manifest.serverVersion());
            }
            if (manifest.metadata() != null) {
                xml.writeEmptyElement("metadata");
                writeOptionalAttribute(xml, "title", manifest.metadata().title());
                writeOptionalAttribute(xml, "author", manifest.metadata().author());
                writeOptionalAttribute(xml, "language", manifest.metadata().language());
            }
            if (!manifest.assets().isEmpty()) {
                xml.writeStartElement("assets");
                for (Asset asset : manifest.assets()) {
                    xml.writeEmptyElement("asset");
                    xml.writeAttribute("path", asset.path());
                    if (asset.sha256() != null) {
                        xml.writeAttribute("sha256", asset.sha256());
                    }
                }
                xml.writeEndElement();
            }

            xml.writeEndElement();
            xml.writeEndDocument();
            xml.flush();
        } catch (XMLStreamException exception) {
            throw new IOException("Could not write " + GamePackageManifest.FILE_NAME + ".", exception);
        }
    }

    /**
     * Reads asset entries from an optional assets element.
     *
     * @param assetsElement assets element, or {@code null}
     * @return a mutable list of validated assets
     * @throws IllegalArgumentException if an unexpected child element is present
     */
    private static List<Asset> readAssets(Element assetsElement) {
        List<Asset> assets = new ArrayList<>();
        if (assetsElement == null) {
            return assets;
        }

        NodeList assetNodes = assetsElement.getChildNodes();
        for (int i = 0; i < assetNodes.getLength(); i++) {
            if (assetNodes.item(i) instanceof Element assetElement) {
                if (!"asset".equals(assetElement.getTagName())) {
                    throw new IllegalArgumentException("Unexpected element in <assets>.");
                }
                String hash = optionalAttribute(assetElement, "sha256");
                assets.add(new Asset(requiredAttribute(assetElement, "path"), hash));
            }
        }

        return assets;
    }

    /**
     * Reads optional metadata from a metadata element.
     *
     * @param metadataElement metadata element, or {@code null}
     * @return parsed metadata, or {@code null} when no element is supplied
     */
    private static PackageMetadata readMetadata(Element metadataElement) {
        if (metadataElement == null) {
            return null;
        }

        return new PackageMetadata(
            optionalAttribute(metadataElement, "title"),
            optionalAttribute(metadataElement, "author"),
            optionalAttribute(metadataElement, "language")
        );
    }

    /**
     * Obtains a required direct child element.
     *
     * @param parent  parent element
     * @param tagName required child tag name
     * @return the non-null matching child
     * @throws IllegalArgumentException if no matching child exists
     */
    private static Element requiredChild(Element parent, String tagName) {
        Element child = optionalChild(parent, tagName);
        if (child == null) {
            throw new IllegalArgumentException("Missing <" + tagName + "> in " + GamePackageManifest.FILE_NAME + ".");
        }
        return child;
    }

    /**
     * Obtains at most one direct child element with a tag name.
     *
     * @param parent  parent element
     * @param tagName child tag name
     * @return the matching child, or {@code null} when absent
     * @throws IllegalArgumentException if multiple matching children exist
     */
    private static Element optionalChild(Element parent, String tagName) {
        Element found = null;
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element element && tagName.equals(element.getTagName())) {
                if (found != null) {
                    throw new IllegalArgumentException(
                        "Duplicate <" + tagName + "> in " + GamePackageManifest.FILE_NAME + "."
                    );
                }
                found = element;
            }
        }

        return found;
    }

    /**
     * Obtains a required, trimmed attribute value.
     *
     * @param element source element
     * @param name    attribute name
     * @return non-blank trimmed attribute value
     * @throws IllegalArgumentException if the attribute is blank or absent
     */
    private static String requiredAttribute(Element element, String name) {
        String value = element.getAttribute(name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return value.trim();
    }

    /**
     * Obtains an optional, trimmed attribute value.
     *
     * @param element source element
     * @param name    attribute name
     * @return trimmed attribute value, or {@code null} when blank or absent
     */
    private static String optionalAttribute(Element element, String name) {
        String value = element.getAttribute(name).trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * Writes a non-blank optional XML attribute.
     *
     * @param xml   XML destination
     * @param name  attribute name
     * @param value value to write, or {@code null}
     * @throws XMLStreamException if the attribute cannot be written
     */
    private static void writeOptionalAttribute(XMLStreamWriter xml, String name, String value)
        throws XMLStreamException {
        if (value != null && !value.isBlank()) {
            xml.writeAttribute(name, value);
        }
    }
}
