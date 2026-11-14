package quantumquest.triviathrone.server.game.creation;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import quantumquest.triviathrone.api.game.GameCreationRequest;
import quantumquest.triviathrone.api.game.GameCreatorInfo;
import quantumquest.triviathrone.api.game.GamePackageManifest;
import quantumquest.triviathrone.api.game.IGameCreator;
import quantumquest.triviathrone.api.game.IGameProvider;
import quantumquest.triviathrone.server.GameProviderRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Lists game creators and generates validated downloadable game packages.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Service
public class GameCreationService {
    /**
     * Holds the providers used by this component.
     */
    private final GameProviderRegistry providers;

    /**
     * Creates a GameCreationService.
     *
     * @param providers the game-provider registry
     */
    public GameCreationService(GameProviderRegistry providers) {
        this.providers = Objects.requireNonNull(providers, "providers");
    }

    /**
     * Deletes a directory tree recursively.
     */
    private static void deleteTree(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    /**
     * Lists package assets while excluding the game data file.
     *
     * @param packageRoot the generated package directory
     * @param dataFile    the relative game data file path
     * @return manifest entries for packaged assets
     * @throws IOException if package files cannot be listed
     */
    private static List<GamePackageManifest.Asset> packageAssets(Path packageRoot, String dataFile)
        throws IOException {
        Path dataPath = Path.of(dataFile).normalize();
        try (Stream<Path> paths = Files.walk(packageRoot)) {
            List<Path> files = paths.filter(Files::isRegularFile).sorted().toList();
            List<GamePackageManifest.Asset> assets = new ArrayList<>();
            for (Path file : files) {
                Path relativePath = packageRoot.relativize(file);
                if (relativePath.equals(dataPath)) {
                    continue;
                }
                String manifestPath = relativePath.toString().replace(file.getFileSystem().getSeparator(), "/");
                assets.add(new GamePackageManifest.Asset(manifestPath, sha256(file)));
            }
            return List.copyOf(assets);
        }
    }

    /**
     * Computes the SHA-256 checksum of a file.
     */
    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new DigestInputStream(Files.newInputStream(file), digest)) {
                input.transferTo(OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    /**
     * Builds package metadata from the game-creation request.
     */
    private static GamePackageManifest.PackageMetadata packageMetadata(GameCreationRequest request) {
        JsonNode data = request.data();
        JsonNode metadata = data.path("meta").isObject() ? data.path("meta") : data;
        return new GamePackageManifest.PackageMetadata(
            optionalText(metadata, "title"),
            optionalText(metadata, "author"),
            optionalText(metadata, "language")
        );
    }

    /**
     * Returns a nonblank optional JSON text field.
     */
    private static String optionalText(JsonNode data, String field) {
        JsonNode value = data.get(field);
        return value != null && value.isTextual() && !value.textValue().isBlank()
            ? value.textValue().trim()
            : null;
    }

    /**
     * Reads this server module version.
     */
    private static String serverVersion() {
        String version = GameCreationService.class.getPackage().getImplementationVersion();
        return version == null || version.isBlank() ? "development" : version;
    }

    /**
     * Returns available game creators ordered by game type.
     *
     * @return available game creators ordered by game type
     */
    public List<GameCreatorInfo> creators() {
        return providers.creators();
    }

    /**
     * Reports whether a game type supplies a custom editor.
     *
     * @param gameType the game type
     * @return whether s custom editor
     */
    public boolean hasCustomEditor(String gameType) {
        return providers.hasCustomEditor(gameType);
    }

    /**
     * Generates and validates a downloadable package for a game creation request.
     *
     * @param gameType the game type
     * @param request  the request payload
     * @return the generated game archive
     * @throws IOException if the requested file operation fails
     */
    public GeneratedGame generate(String gameType, GameCreationRequest request) throws IOException {
        IGameProvider provider = providers.providerForType(gameType)
            .orElseThrow(() -> new NoSuchElementException("Unknown game type: " + gameType));
        IGameCreator creator = provider.creator()
            .orElseThrow(() -> new NoSuchElementException("Game type does not provide a creator: " + gameType));

        Path temporaryDirectory = Files.createTempDirectory("triviathrone-game-");

        try {
            Path gameFiles = Files.createDirectory(temporaryDirectory.resolve("files"));

            try (DirectoryGameFileWriter writer = new DirectoryGameFileWriter(gameFiles)) {
                creator.create(request, writer);
            }

            List<GamePackageManifest.Asset> assets = packageAssets(gameFiles, provider.gameDataFile());
            providers.packageManifestForType(gameType, serverVersion(), packageMetadata(request), assets)
                .write(gameFiles);
            GamePackageManifest manifest = GamePackageManifest.read(gameFiles);
            if (providers.providerForPackage(manifest).orElse(null) != provider) {
                throw new IllegalArgumentException("The generated package does not match its registered provider.");
            }

            if (provider.load(gameFiles).isEmpty()) {
                throw new IllegalArgumentException("The creator did not produce a loadable game.");
            }

            List<Path> files;
            try (Stream<Path> paths = Files.walk(gameFiles)) {
                files = paths.filter(Files::isRegularFile)
                    .sorted()
                    .toList();
            }

            if (files.isEmpty()) {
                throw new IllegalArgumentException("The creator produced no game files.");
            }

            Path archive = temporaryDirectory.resolve("generated.game.throne");
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
                for (Path file : files) {
                    String entryName = gameFiles.relativize(file)
                        .toString()
                        .replace(file.getFileSystem().getSeparator(), "/");
                    zip.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zip);
                    zip.closeEntry();
                }
            }

            String filename = gameType.replaceAll("[^A-Za-z0-9._-]", "_") + ".game.throne";
            return new GeneratedGame(temporaryDirectory, archive, filename);
        } catch (IOException | RuntimeException exception) {
            try {
                deleteTree(temporaryDirectory);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    /**
     * Owns a generated game archive and deletes its temporary files when closed.
     *
     * @param temporaryDirectory the generated package directory
     * @param archive            the downloadable archive path
     * @param filename           the archive download filename
     * @author TiJ
     * @since 2.0.0
     */
    public record GeneratedGame(Path temporaryDirectory, Path archive, String filename) implements AutoCloseable {
        /**
         * Closes this resource and releases its open streams.
         */
        @Override
        public void close() throws IOException {
            deleteTree(temporaryDirectory);
        }
    }

}
