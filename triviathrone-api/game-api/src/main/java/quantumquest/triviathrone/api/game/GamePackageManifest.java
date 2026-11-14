package quantumquest.triviathrone.api.game;

import quantumquest.triviathrone.api.game.internal.GamePackageManifestXml;

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
        return GamePackageManifestXml.read(packageRoot);
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
        GamePackageManifestXml.write(this, output);
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

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
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
