package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.game.GameDefinition;
import de.quantumquest.triviathrone.game.GameDefinitionLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class LobbyGameFiles {
    private final Path storageDir;

    public LobbyGameFiles(@Value("${triviathrone.games.storage-dir:upload/games}") String storageDir) {
        this.storageDir = Path.of(Objects.requireNonNull(storageDir, "storageDir"));
    }

    public Optional<GameDefinition> extractUploadedGame(String lobbyCode, MultipartFile file) throws IOException {
        Objects.requireNonNull(lobbyCode, "lobbyCode");
        Objects.requireNonNull(file, "file");

        Path storageRoot = storageDir.toAbsolutePath().normalize();
        Files.createDirectories(storageRoot);
        Path lobbyDir = storageRoot.resolve(lobbyCode).normalize();
        if (!lobbyDir.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Invalid lobby code.");
        }
        Files.createDirectories(lobbyDir);

        boolean manifestFound = false;
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

                if (entry.getName().equals("manifest.xml")) {
                    manifestFound = true;
                }

                Files.createDirectories(destination.getParent());
                Files.copy(zip, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        if (!manifestFound) {
            return Optional.empty();
        }
        return Optional.of(GameDefinitionLoader.parse(lobbyDir));
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
}
