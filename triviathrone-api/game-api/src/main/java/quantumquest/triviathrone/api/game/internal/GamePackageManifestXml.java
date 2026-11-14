package quantumquest.triviathrone.api.game.internal;

import quantumquest.triviathrone.api.game.GamePackageManifest;
import quantumquest.triviathrone.api.game.GamePackageManifest.Asset;
import quantumquest.triviathrone.api.game.GamePackageManifest.PackageMetadata;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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

public final class GamePackageManifestXml {
    private static final String LEGACY_MANIFEST_VERSION = "1.0";
    private static final int MAX_MANIFEST_SIZE = 1024 * 1024;

    private GamePackageManifestXml() {
    }

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

    private static Element requiredChild(Element parent, String tagName) {
        Element child = optionalChild(parent, tagName);
        if (child == null) {
            throw new IllegalArgumentException("Missing <" + tagName + "> in " + GamePackageManifest.FILE_NAME + ".");
        }
        return child;
    }

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

    private static String requiredAttribute(Element element, String name) {
        String value = element.getAttribute(name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return value.trim();
    }

    private static String optionalAttribute(Element element, String name) {
        String value = element.getAttribute(name).trim();
        return value.isEmpty() ? null : value;
    }

    private static void writeOptionalAttribute(XMLStreamWriter xml, String name, String value)
            throws XMLStreamException {
        if (value != null && !value.isBlank()) {
            xml.writeAttribute(name, value);
        }
    }
}
