package quantumquest.triviathrone.server;

import quantumquest.triviathrone.api.game.IGameDefinition;
import quantumquest.triviathrone.api.game.IGameProvider;
import quantumquest.triviathrone.api.game.GamePackageManifest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class LobbyGameFiles {
    private final Path storageDir;
    private final GameProviderRegistry gameProviders;

    public LobbyGameFiles(
            @Value("${triviathrone.games.storage-dir:upload/games}") String storageDir,
            GameProviderRegistry gameProviders
    ) {
        this.storageDir = Path.of(Objects.requireNonNull(storageDir, "storageDir"));
        this.gameProviders = Objects.requireNonNull(gameProviders, "gameProviders");
    }

    public boolean supportsGameFile(String filename) {
        if (filename == null) {
            return false;
        }
        return gameProviders.providerForFile(filename).isPresent();
    }

    public Optional<IGameDefinition> extractUploadedGame(
            String lobbyCode,
            String filename,
            MultipartFile file
    ) throws IOException {
        Objects.requireNonNull(lobbyCode, "lobbyCode");
        Objects.requireNonNull(filename, "filename");
        Objects.requireNonNull(file, "file");
        if (gameProviders.providerForFile(filename).isEmpty()) {
            throw new IllegalArgumentException("Unsupported game file.");
        }

        Path storageRoot = storageDir.toAbsolutePath().normalize();
        Files.createDirectories(storageRoot);
        Path lobbyDir = storageRoot.resolve(lobbyCode).normalize();
        if (!lobbyDir.startsWith(storageRoot) || lobbyDir.equals(storageRoot)) {
            throw new IllegalArgumentException("Invalid lobby code.");
        }

        Path stagingDir = Files.createTempDirectory(storageRoot, ".game-upload-");
        try {
            Set<Path> extractedEntries = new HashSet<>();
            try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    Path relativePath = safeZipEntryPath(entry.getName());
                    if (!extractedEntries.add(relativePath)) {
                        throw new IllegalArgumentException("Duplicate zip entry.");
                    }
                    Path destination = stagingDir.resolve(relativePath).normalize();
                    if (!destination.startsWith(stagingDir) || destination.equals(stagingDir)) {
                        throw new IllegalArgumentException("Invalid zip entry.");
                    }

                    if (entry.isDirectory()) {
                        Files.createDirectories(destination);
                        continue;
                    }

                    Files.createDirectories(destination.getParent());
                    Files.copy(zip, destination);
                }
            }

            GamePackageManifest manifest = GamePackageManifest.read(stagingDir);
            IGameProvider provider = gameProviders.providerForPackage(manifest)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No installed provider matches the game package manifest."
                    ));
            if (!provider.supportsFile(filename)) {
                throw new IllegalArgumentException("The game package provider does not support this file type.");
            }

            if (Files.exists(lobbyDir, LinkOption.NOFOLLOW_LINKS)) {
                deleteDirectory(lobbyDir);
            }
            Files.move(stagingDir, lobbyDir, StandardCopyOption.ATOMIC_MOVE);
            return provider.load(lobbyDir);
        } finally {
            if (Files.exists(stagingDir, LinkOption.NOFOLLOW_LINKS)) {
                deleteDirectory(stagingDir);
            }
        }
    }

    private static Path safeZipEntryPath(String entryName) {
        if (entryName == null
                || entryName.isBlank()
                || entryName.contains("\\")
                || entryName.contains("//")
                || entryName.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("Invalid zip entry.");
        }
        Path path;
        try {
            path = Path.of(entryName);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid zip entry.", exception);
        }
        if (path.isAbsolute()) {
            throw new IllegalArgumentException("Invalid zip entry.");
        }
        for (Path component : path) {
            if (component.toString().isBlank()
                    || component.toString().equals(".")
                    || component.toString().equals("..")) {
                throw new IllegalArgumentException("Invalid zip entry.");
            }
        }
        return path;
    }

    public Optional<Path> findMediaFile(String lobbyCode, String mediaPath) throws IOException {
        Objects.requireNonNull(lobbyCode, "lobbyCode");
        Objects.requireNonNull(mediaPath, "mediaPath");

        Path storageRoot = storageDir.toAbsolutePath().normalize();
        Path lobbyDir = storageRoot.resolve(lobbyCode).normalize();
        Path mediaFile = lobbyDir.resolve(mediaPath).normalize();
        if (!lobbyDir.startsWith(storageRoot)
                || !mediaFile.startsWith(lobbyDir)
                || !Files.isRegularFile(mediaFile)) {
            return Optional.empty();
        }

        Path realLobbyDir = lobbyDir.toRealPath();
        Path realMediaFile = mediaFile.toRealPath();
        if (!realMediaFile.startsWith(realLobbyDir)) {
            return Optional.empty();
        }
        return Optional.of(realMediaFile);
    }

    public void deleteLobbyFiles(String lobbyCode) throws IOException {
        Objects.requireNonNull(lobbyCode, "lobbyCode");

        Path storageRoot = storageDir.toAbsolutePath().normalize();
        Path lobbyDir = storageRoot.resolve(lobbyCode).normalize();
        if (!lobbyDir.startsWith(storageRoot) || lobbyDir.equals(storageRoot)) {
            throw new IllegalArgumentException("Invalid lobby code.");
        }
        if (!Files.exists(lobbyDir, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }

        deleteDirectory(lobbyDir);
    }

    private static void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
                if (exception != null) {
                    throw exception;
                }
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
