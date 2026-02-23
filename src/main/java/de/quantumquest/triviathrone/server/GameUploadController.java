package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.auth.UserAccount;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Controller
@RequestMapping("/api/game")
public class GameUploadController {
    private static final Logger logger = LoggerFactory.getLogger(GameUploadController.class);

    private final Path storageDir = Paths.get("upload/games");
    private final LobbyService lobbyService;
    private final LobbyEventService lobbyEventService;

    public GameUploadController(LobbyService lobbyService, LobbyEventService lobbyEventService) {
        this.lobbyService = lobbyService;
        this.lobbyEventService = lobbyEventService;

        clear();
    }

    private void clear() {
        try {
            FileUtils.deleteDirectory(storageDir.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<?> uploadGame(
            @RequestParam("file") MultipartFile file,
            @RequestParam("lobbyCode") String lobbyCode,
            @AuthenticationPrincipal UserAccount user) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty.");
        }

        if (!Objects.requireNonNull(file.getOriginalFilename()).endsWith(".game.throne")) {
            return ResponseEntity.badRequest().body("Invalid file type.");
        }

        Optional<Lobby> optLobby = lobbyService.getLobby(lobbyCode);

        if (optLobby.isEmpty()) {
            return ResponseEntity.badRequest().body("Lobby does not exist");
        }

        String lobbyHostUsername = optLobby.get().getHostUsername();
        if (!lobbyHostUsername.equals(user.getUsername())) {
            return ResponseEntity.status(403).body("Only host can upload.");
        }

        try {
            Path lobbyDir = storageDir.resolve(lobbyCode).normalize().toAbsolutePath();
            Files.createDirectories(lobbyDir);

            try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                boolean manifestFound = false;

                while ((entry = zis.getNextEntry()) != null) {
                    Path newFile = lobbyDir.resolve(entry.getName()).normalize();

                    if (!newFile.startsWith(lobbyDir)) {
                        throw new RuntimeException("Invalid zip entry.");
                    }

                    if (entry.isDirectory()) {
                        Files.createDirectories(newFile);
                        continue;
                    }

                    if (entry.getName().equals("manifest.xml")) {
                        manifestFound = true;
                    }

                    Files.createDirectories(newFile.getParent());
                    Files.copy(zis, newFile, StandardCopyOption.REPLACE_EXISTING);
                }

                if (!manifestFound) {
                    return ResponseEntity.badRequest().body("manifest.xml missing");
                }
            }

            logger.info("[{}] {} uploaded game \"{}\".", lobbyCode, lobbyHostUsername, file.getOriginalFilename());
            lobbyEventService.hostUploadedGame(lobbyCode, lobbyHostUsername, file.getOriginalFilename());
            return ResponseEntity.ok("Game uploaded successfully.");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Upload failed");
        }
    }
}
