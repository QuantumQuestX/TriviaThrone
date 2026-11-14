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
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Describes the contents and compatibility requirements of a game package.
 *
 * <p>The asset list is an immutable snapshot. Metadata and the optional server version may be
 * {@code null}. Package paths use forward slashes and are relative to the package root.</p>
 *
 * @param manifestVersion       supported manifest version
 * @param modId                 non-blank mod identifier
 * @param modVersion            non-blank mod version
 * @param apiVersion            non-blank API version required by the package
 * @param gameType              non-blank game-type identifier
 * @param providerClass         non-blank provider class name
 * @param gameDataFile          non-blank relative data-file path
 * @param gameDataFormatVersion non-blank game-data format version
 * @param serverVersion         server version that created the package, or {@code null}
 * @param metadata              optional package metadata
 * @param assets                non-null immutable snapshot of bundled assets
 * @author TiJ
 * @since 2.0.0
 */
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
    /**
     * Standard filename of a package manifest.
     */
    public static final String FILE_NAME = "manifest.xml";
    /**
     * Current supported manifest format version.
     */
    public static final String CURRENT_MANIFEST_VERSION = "2.0";
    /**
     * Legacy supported manifest format version.
     */
    private static final String LEGACY_MANIFEST_VERSION = "1.0";
    /**
     * Standard message-digest algorithm used for asset integrity checks.
     */
    private static final String SHA_256 = "SHA-256";

    /**
     * Validates manifest components and snapshots the asset list.
     *
     * @throws NullPointerException     if {@code assets} is null
     * @throws IllegalArgumentException if required values are invalid, a path is unsafe, a version
     *                                  is unsupported, or asset paths are duplicated
     */
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

    /**
     * Reads and validates the manifest and files in a game package.
     *
     * @param packageRoot non-null package root
     * @return the validated immutable manifest
     * @throws IOException              if a package file cannot be read
     * @throws IllegalArgumentException if the package is malformed or fails validation
     */
    public static GamePackageManifest read(Path packageRoot) throws IOException {
        Objects.requireNonNull(packageRoot, "packageRoot");
        return GamePackageManifestXml.read(packageRoot);
    }

    /**
     * Validates a package file and, when provided, its SHA-256 digest.
     *
     * @param root         normalized package root
     * @param realRoot     resolved package root
     * @param relativePath validated package-relative path
     * @param expectedHash expected SHA-256 digest, or {@code null}
     * @throws IOException              if the file cannot be resolved or read
     * @throws IllegalArgumentException if the file is unsafe, missing, or has a mismatched digest
     */
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

    /**
     * Resolves a validated package-relative path beneath a root.
     *
     * @param root         normalized package root
     * @param relativePath path to resolve
     * @return the non-null normalized resolved path
     * @throws IllegalArgumentException if the path is invalid or escapes the root
     */
    private static Path resolveRelativeFile(Path root, String relativePath) {
        requireRelativePath(relativePath);
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root) || resolved.equals(root)) {
            throw new IllegalArgumentException("Invalid package path: " + relativePath);
        }
        return resolved;
    }

    /**
     * Verifies that a path is a non-empty, forward-slash-delimited package-relative path.
     *
     * @param path path to validate
     * @throws IllegalArgumentException if the path is null, blank, absolute, or contains unsafe parts
     */
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

    /**
     * Verifies that a required text value is non-blank.
     *
     * @param value value to validate
     * @param name  value name for error reporting
     * @throws IllegalArgumentException if the value is null or blank
     */
    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
    }

    /**
     * Writes this manifest as a new manifest file in a package root.
     *
     * @param packageRoot non-null package root
     * @throws IOException                              if the file cannot be created or written
     * @throws java.nio.file.FileAlreadyExistsException if a manifest already exists
     */
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

    /**
     * Serializes this manifest as XML to a caller-owned output stream.
     *
     * <p>This method does not close the stream.</p>
     *
     * @param output non-null destination stream
     * @throws IOException if XML serialization fails
     */
    public void write(OutputStream output) throws IOException {
        GamePackageManifestXml.write(this, output);
    }

    /**
     * Validates that declared package files exist within the package root and match declared hashes.
     *
     * @param packageRoot non-null package root
     * @throws IOException              if a file cannot be resolved or read
     * @throws IllegalArgumentException if a file is missing, unsafe, or has an invalid hash
     */
    public void validateFiles(Path packageRoot) throws IOException {
        Path root = packageRoot.toAbsolutePath().normalize();
        Path realRoot = root.toRealPath();
        verifyBundledFile(root, realRoot, gameDataFile, null);
        for (Asset asset : assets) {
            verifyBundledFile(root, realRoot, asset.path(), asset.sha256());
        }
    }

    /**
     * Provides optional descriptive metadata for a game package.
     *
     * @param title    optional package title
     * @param author   optional package author
     * @param language optional package language
     * @author TiJ
     * @since 2.0.0
     */
    public record PackageMetadata(String title, String author, String language) {
    }

    /**
     * Identifies an asset bundled in a game package.
     *
     * @param path   non-blank relative package path
     * @param sha256 optional 64-character hexadecimal SHA-256 digest
     * @author TiJ
     * @since 2.0.0
     */
    public record Asset(String path, String sha256) {
        /**
         * Validates the asset path and optional digest.
         *
         * @throws IllegalArgumentException if the path is invalid, names the manifest, or the digest
         *                                  is not a SHA-256 hexadecimal value
         */
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
