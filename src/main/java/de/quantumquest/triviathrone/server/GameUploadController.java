package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.auth.UserAccount;
import de.quantumquest.triviathrone.game.GameDefinitionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
@RequestMapping("/api/game")
public class GameUploadController {
    private static final Logger logger = LoggerFactory.getLogger(GameUploadController.class);
    private static final Path STORAGE_DIR = Path.of("upload", "games");

    private final LobbyService lobbyService;
    private final LobbyEventService lobbyEventService;

    public GameUploadController(LobbyService lobbyService, LobbyEventService lobbyEventService) {
        this.lobbyService = lobbyService;
        this.lobbyEventService = lobbyEventService;
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadGame(
            @RequestParam("file") MultipartFile file,
            @RequestParam("lobbyCode") String lobbyCode,
            @AuthenticationPrincipal UserAccount user
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null
                || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".game.throne")) {
            return ResponseEntity.badRequest().body("Invalid file type.");
        }

        Lobby lobby = lobbyService.getLobby(lobbyCode).orElse(null);
        if (lobby == null) {
            return ResponseEntity.badRequest().body("Lobby does not exist");
        }

        String hostUsername = lobby.getHostUsername();
        if (!hostUsername.equals(user.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only host can upload.");
        }

        try {
            Path storageRoot = STORAGE_DIR.toAbsolutePath().normalize();
            Files.createDirectories(storageRoot);
            Path lobbyDir = storageRoot.resolve(lobbyCode).normalize();
            if (!lobbyDir.startsWith(storageRoot)) {
                return ResponseEntity.badRequest().body("Invalid lobby code.");
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
                return ResponseEntity.badRequest().body("manifest.xml missing");
            }

            lobby.loadGame(GameDefinitionLoader.parse(lobbyDir));
            logger.info("[{}] {} uploaded game \"{}\".", lobbyCode, hostUsername, originalFilename);
            lobbyEventService.hostUploadedGame(lobbyCode, hostUsername, originalFilename);
            return ResponseEntity.ok("Game uploaded successfully.");
        } catch (IOException exception) {
            logger.error("[{}] Could not store uploaded game", lobbyCode, exception);
            return ResponseEntity.internalServerError().body("Could not store game file.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            logger.warn("[{}] Could not process uploaded game", lobbyCode, exception);
            return ResponseEntity.badRequest().body("Could not process game file: " + exception.getMessage());
        }
    }
}
