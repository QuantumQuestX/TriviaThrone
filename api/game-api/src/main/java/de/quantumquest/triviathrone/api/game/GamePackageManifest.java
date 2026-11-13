package de.quantumquest.triviathrone.api.game;

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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record GamePackageManifest(
        String manifestVersion,
        String modId,
        String modVersion,
        String apiVersion,
        String gameType,
        String providerClass,
        String gameDataFile,
        String gameDataFormatVersion,
        String serverVersion,
        PackageMetadata metadata,
        List<Asset> assets
) {
    public static final String FILE_NAME = "manifest.xml";
    public static final String CURRENT_MANIFEST_VERSION = "2.0";
    private static final String LEGACY_MANIFEST_VERSION = "1.0";
    private static final int MAX_MANIFEST_SIZE = 1024 * 1024;
    private static final String SHA_256 = "SHA-256";

    public GamePackageManifest {
        requireText(manifestVersion, "manifestVersion");
        if (!CURRENT_MANIFEST_VERSION.equals(manifestVersion)
                && !LEGACY_MANIFEST_VERSION.equals(manifestVersion)) {
            throw new IllegalArgumentException("Unsupported manifest version: " + manifestVersion);
        }
        requireText(modId, "modId");
        requireText(modVersion, "modVersion");
        requireText(apiVersion, "apiVersion");
        requireText(gameType, "gameType");
        requireText(providerClass, "providerClass");
        requireRelativePath(gameDataFile);
        if (FILE_NAME.equals(gameDataFile)) {
            throw new IllegalArgumentException("The game-data file must be separate from " + FILE_NAME + ".");
        }
        requireText(gameDataFormatVersion, "gameDataFormatVersion");
        if (serverVersion != null && serverVersion.isBlank()) {
            serverVersion = null;
        }
        assets = List.copyOf(Objects.requireNonNull(assets, "assets"));
        Set<String> assetPaths = new HashSet<>();
        for (Asset asset : assets) {
            if (!assetPaths.add(asset.path())) {
                throw new IllegalArgumentException("Duplicate asset path: " + asset.path());
            }
        }
    }

    public static GamePackageManifest read(Path packageRoot) throws IOException {
        Objects.requireNonNull(packageRoot, "packageRoot");
        Path packageDirectory = packageRoot.toAbsolutePath().normalize();
        Path manifestFile = packageDirectory.resolve(FILE_NAME);
        if (!Files.isRegularFile(manifestFile, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException(FILE_NAME + " is required in a game package.");
        }
        Path realRoot = packageDirectory.toRealPath();
        if (!manifestFile.toRealPath().startsWith(realRoot)) {
            throw new IllegalArgumentException(FILE_NAME + " resolves outside the game package.");
        }

        byte[] xml;
        try (InputStream input = Files.newInputStream(manifestFile)) {
            xml = input.readNBytes(MAX_MANIFEST_SIZE + 1);
        }
        if (xml.length > MAX_MANIFEST_SIZE) {
            throw new IllegalArgumentException(FILE_NAME + " exceeds the maximum size.");
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

            Document document = factory.newDocumentBuilder()
                    .parse(new java.io.ByteArrayInputStream(xml));
            Element root = document.getDocumentElement();
            if (!"gamePackage".equals(root.getTagName())) {
                throw new IllegalArgumentException("The package manifest root must be <gamePackage>.");
            }

            String manifestVersion = requiredAttribute(root, "manifestVersion");
            Element mod;
            if (CURRENT_MANIFEST_VERSION.equals(manifestVersion)) {
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

            List<Asset> assets = new ArrayList<>();
            if (assetsElement != null) {
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
            }

            PackageMetadata metadata = metadataElement == null
                    ? null
                    : new PackageMetadata(
                            optionalAttribute(metadataElement, "title"),
                            optionalAttribute(metadataElement, "author"),
                            optionalAttribute(metadataElement, "language")
                    );

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
            throw new IllegalArgumentException("Could not parse " + FILE_NAME + ".", exception);
        }
    }

    public void write(Path packageRoot) throws IOException {
        Objects.requireNonNull(packageRoot, "packageRoot");
        Path manifestFile = packageRoot.resolve(FILE_NAME);
        try (OutputStream output = Files.newOutputStream(
                manifestFile,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        )) {
            write(output);
        }
    }

    public void write(OutputStream output) throws IOException {
        Objects.requireNonNull(output, "output");
        try {
            XMLStreamWriter xml = XMLOutputFactory.newFactory().createXMLStreamWriter(output, "UTF-8");
            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeStartElement("gamePackage");
            xml.writeAttribute("manifestVersion", manifestVersion);
            xml.writeEmptyElement(CURRENT_MANIFEST_VERSION.equals(manifestVersion) ? "mod" : "plugin");
            xml.writeAttribute("id", modId);
            xml.writeAttribute("version", modVersion);
            xml.writeAttribute("apiVersion", apiVersion);
            xml.writeEmptyElement("game");
            xml.writeAttribute("type", gameType);
            xml.writeAttribute("providerClass", providerClass);
            xml.writeAttribute("dataFile", gameDataFile);
            xml.writeAttribute("formatVersion", gameDataFormatVersion);
            if (serverVersion != null) {
                xml.writeEmptyElement("createdBy");
                xml.writeAttribute("serverVersion", serverVersion);
            }
            if (metadata != null) {
                xml.writeEmptyElement("metadata");
                writeOptionalAttribute(xml, "title", metadata.title());
                writeOptionalAttribute(xml, "author", metadata.author());
                writeOptionalAttribute(xml, "language", metadata.language());
            }
            if (!assets.isEmpty()) {
                xml.writeStartElement("assets");
                for (Asset asset : assets) {
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
            throw new IOException("Could not write " + FILE_NAME + ".", exception);
        }
    }

    public void validateFiles(Path packageRoot) throws IOException {
        Path root = packageRoot.toAbsolutePath().normalize();
        Path realRoot = root.toRealPath();
        verifyBundledFile(root, realRoot, gameDataFile, null);
        for (Asset asset : assets) {
            verifyBundledFile(root, realRoot, asset.path(), asset.sha256());
        }
    }

    private static void verifyBundledFile(Path root, Path realRoot, String relativePath, String expectedHash)
            throws IOException {
        Path file = resolveRelativeFile(root, relativePath);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Package file is missing or is not a regular file: " + relativePath);
        }
        Path realFile = file.toRealPath();
        if (!realFile.startsWith(realRoot)) {
            throw new IllegalArgumentException("Package file resolves outside the package: " + relativePath);
        }
        if (expectedHash != null) {
            String actualHash;
            try {
                MessageDigest digest = MessageDigest.getInstance(SHA_256);
                try (InputStream input = new DigestInputStream(Files.newInputStream(realFile), digest)) {
                    input.transferTo(OutputStream.nullOutputStream());
                }
                actualHash = HexFormat.of().formatHex(digest.digest());
            } catch (NoSuchAlgorithmException exception) {
                throw new IllegalStateException("SHA-256 is not available.", exception);
            }
            if (!actualHash.equalsIgnoreCase(expectedHash)) {
                throw new IllegalArgumentException("SHA-256 mismatch for package file: " + relativePath);
            }
        }
    }

    private static Path resolveRelativeFile(Path root, String relativePath) {
        requireRelativePath(relativePath);
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root) || resolved.equals(root)) {
            throw new IllegalArgumentException("Invalid package path: " + relativePath);
        }
        return resolved;
    }

    private static void requireRelativePath(String path) {
        requireText(path, "package path");
        if (path.contains("\\")
                || path.contains("//")
                || path.startsWith("/")
                || path.endsWith("/")
                || path.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("Package paths must be relative and use forward slashes.");
        }
        Path parsed;
        try {
            parsed = Path.of(path);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid package path: " + path, exception);
        }
        if (parsed.isAbsolute() || parsed.getNameCount() == 0) {
            throw new IllegalArgumentException("Package paths must be relative.");
        }
        for (Path component : parsed) {
            if (component.toString().isBlank()
                    || component.toString().equals(".")
                    || component.toString().equals("..")) {
                throw new IllegalArgumentException("Invalid package path: " + path);
            }
        }
    }

    private static Element requiredChild(Element parent, String tagName) {
        Element child = optionalChild(parent, tagName);
        if (child == null) {
            throw new IllegalArgumentException("Missing <" + tagName + "> in " + FILE_NAME + ".");
        }
        return child;
    }

    private static Element optionalChild(Element parent, String tagName) {
        Element found = null;
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element element && tagName.equals(element.getTagName())) {
                if (found != null) {
                    throw new IllegalArgumentException("Duplicate <" + tagName + "> in " + FILE_NAME + ".");
                }
                found = element;
            }
        }
        return found;
    }

    private static String requiredAttribute(Element element, String name) {
        String value = element.getAttribute(name);
        requireText(value, name);
        return value.trim();
    }

    private static String optionalAttribute(Element element, String name) {
        String value = element.getAttribute(name).trim();
        return value.isEmpty() ? null : value;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
    }

    private static void writeOptionalAttribute(XMLStreamWriter xml, String name, String value)
            throws XMLStreamException {
        if (value != null && !value.isBlank()) {
            xml.writeAttribute(name, value);
        }
    }

    public record PackageMetadata(String title, String author, String language) {
    }

    public record Asset(String path, String sha256) {
        public Asset {
            requireRelativePath(path);
            if (FILE_NAME.equals(path)) {
                throw new IllegalArgumentException("The package manifest cannot be listed as an asset.");
            }
            if (sha256 != null && !sha256.matches("[0-9a-fA-F]{64}")) {
                throw new IllegalArgumentException("Asset SHA-256 must contain 64 hexadecimal characters.");
            }
        }
    }

}
