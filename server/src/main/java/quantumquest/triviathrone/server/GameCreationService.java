package quantumquest.triviathrone.server;

import quantumquest.triviathrone.api.game.GameCreationRequest;
import quantumquest.triviathrone.api.game.GameCreatorInfo;
import quantumquest.triviathrone.api.game.GameFileWriter;
import quantumquest.triviathrone.api.game.GamePackageManifest;
import quantumquest.triviathrone.api.game.IGameCreator;
import quantumquest.triviathrone.api.game.IGameProvider;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.DigestInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class GameCreationService {
    private final GameProviderRegistry providers;

    public GameCreationService(GameProviderRegistry providers) {
        this.providers = Objects.requireNonNull(providers, "providers");
    }

    public List<GameCreatorInfo> creators() {
        return providers.creators();
    }

    public boolean hasCustomEditor(String gameType) {
        return providers.hasCustomEditor(gameType);
    }

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

    private static GamePackageManifest.PackageMetadata packageMetadata(GameCreationRequest request) {
        JsonNode data = request.data();
        JsonNode metadata = data.path("meta").isObject() ? data.path("meta") : data;
        return new GamePackageManifest.PackageMetadata(
                optionalText(metadata, "title"),
                optionalText(metadata, "author"),
                optionalText(metadata, "language")
        );
    }

    private static String optionalText(JsonNode data, String field) {
        JsonNode value = data.get(field);
        return value != null && value.isTextual() && !value.textValue().isBlank()
                ? value.textValue().trim()
                : null;
    }

    private static String serverVersion() {
        String version = GameCreationService.class.getPackage().getImplementationVersion();
        return version == null || version.isBlank() ? "development" : version;
    }

    public record GeneratedGame(Path temporaryDirectory, Path archive, String filename) implements AutoCloseable {
        @Override
        public void close() throws IOException {
            deleteTree(temporaryDirectory);
        }
    }

    private static final class DirectoryGameFileWriter implements GameFileWriter, Closeable {
        private final Path root;
        private final Set<OutputStream> openStreams = new HashSet<>();

        private DirectoryGameFileWriter(Path root) {
            this.root = root.toAbsolutePath().normalize();
        }

        @Override
        public OutputStream createFile(String relativePath) throws IOException {
            if (relativePath == null || relativePath.isBlank() || relativePath.contains("\\")) {
                throw new IllegalArgumentException("Invalid game-file path.");
            }

            Path relative = Path.of(relativePath);
            if (relative.isAbsolute() || relative.getNameCount() == 0) {
                throw new IllegalArgumentException("Game-file paths must be relative.");
            }

            for (Path component : relative) {
                if (component.toString().equals("..") || component.toString().equals(".")) {
                    throw new IllegalArgumentException("Invalid game-file path.");
                }
            }

            Path target = root.resolve(relative).normalize();
            if (!target.startsWith(root) || target.equals(root)) {
                throw new IllegalArgumentException("Invalid game-file path.");
            }

            Files.createDirectories(target.getParent());
            OutputStream delegate = Files.newOutputStream(
                    target,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );

            OutputStream tracked = new FilterOutputStream(delegate) {
                private boolean closed;

                @Override
                public void close() throws IOException {
                    if (closed) {
                        return;
                    }

                    closed = true;

                    try {
                        super.close();
                    } finally {
                        openStreams.remove(this);
                    }
                }
            };
            openStreams.add(tracked);
            return tracked;
        }

        @Override
        public void close() throws IOException {
            IOException failure = null;

            for (OutputStream stream : new ArrayList<>(openStreams)) {
                try {
                    stream.close();
                } catch (IOException exception) {
                    if (failure == null) {
                        failure = exception;
                    } else {
                        failure.addSuppressed(exception);
                    }
                }
            }

            if (failure != null) {
                throw failure;
            }
        }
    }
}
