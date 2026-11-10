package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.api.game.IGameDefinition;
import de.quantumquest.triviathrone.api.game.IGameProvider;
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
import java.util.Objects;
import java.util.Optional;
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
        IGameProvider provider = gameProviders.providerForFile(filename)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported game file."));

        Path storageRoot = storageDir.toAbsolutePath().normalize();
        Files.createDirectories(storageRoot);
        Path lobbyDir = storageRoot.resolve(lobbyCode).normalize();
        if (!lobbyDir.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Invalid lobby code.");
        }
        Files.createDirectories(lobbyDir);

        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path destination = lobbyDir.resolve(entry.getName()).normalize();
                if (!destination.startsWith(lobbyDir)) {
                    throw new IllegalArgumentException("Invalid zip entry.");
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                    continue;
                }

                Files.createDirectories(destination.getParent());
                Files.copy(zip, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        return provider.load(lobbyDir);
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

        Files.walkFileTree(lobbyDir, new SimpleFileVisitor<>() {
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
