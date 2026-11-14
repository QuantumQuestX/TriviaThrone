package quantumquest.triviathrone.server;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import quantumquest.triviathrone.api.game.IGameDefinition;
import quantumquest.triviathrone.auth.UserAccount;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles authorized game-package uploads and game-media retrieval.
 *
 * @author TiJ
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/game")
public class GameUploadController {
    /**
     * Logs events from this component.
     */
    private static final Logger logger = LoggerFactory.getLogger(GameUploadController.class);

    /**
     * Holds the lobb ervice used by this component.
     */
    private final LobbyService lobbyService;
    /**
     * Holds the lobb ven ervice used by this component.
     */
    private final LobbyEventService lobbyEventService;
    /**
     * Holds the lobb am iles used by this component.
     */
    private final LobbyGameFiles lobbyGameFiles;

    /**
     * Creates a GameUploadController.
     *
     * @param lobbyService      the lobby service
     * @param lobbyEventService the lobby event service
     * @param lobbyGameFiles    the lobby game files
     */
    public GameUploadController(
        LobbyService lobbyService,
        LobbyEventService lobbyEventService,
        LobbyGameFiles lobbyGameFiles
    ) {
        this.lobbyService = Objects.requireNonNull(lobbyService, "lobbyService");
        this.lobbyEventService = Objects.requireNonNull(lobbyEventService, "lobbyEventService");
        this.lobbyGameFiles = Objects.requireNonNull(lobbyGameFiles, "lobbyGameFiles");
    }

    /**
     * Serves a media file referenced by the lobby's loaded game to one of its members.
     *
     * @param lobbyCode code of the lobby that owns the media
     * @param mediaPath package-relative path of the requested media
     * @param user      authenticated user requesting the media
     * @return the detected media resource
     * @throws IOException if the stored media cannot be read
     */
    @GetMapping("/{lobbyCode}/media")
    public ResponseEntity<Resource> getGameMedia(
        @PathVariable String lobbyCode,
        @RequestParam("path") String mediaPath,
        @AuthenticationPrincipal UserAccount user
    ) throws IOException {
        Objects.requireNonNull(user, "user");
        Lobby lobby = lobbyService.getLobby(lobbyCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!lobby.listUsernames().contains(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        IGameDefinition definition = lobby.getGameDefinition();
        boolean referencedByGame = definition != null && definition.referencesMedia(mediaPath);
        if (!referencedByGame) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Path realMediaFile = lobbyGameFiles.findMediaFile(lobbyCode, mediaPath)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String contentType;
        try (InputStream media = Files.newInputStream(realMediaFile)) {
            contentType = new Tika().detect(media, realMediaFile.getFileName().toString());
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .body(new FileSystemResource(realMediaFile));
    }

    /**
     * Validates, stores, and loads a game package uploaded by a lobby host.
     *
     * @param file      the file
     * @param lobbyCode the lobby code
     * @param user      the user account
     * @return the upload outcome
     */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadGame(
        @RequestParam("file") MultipartFile file,
        @RequestParam("lobbyCode") String lobbyCode,
        @AuthenticationPrincipal UserAccount user
    ) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(lobbyCode, "lobbyCode");
        Objects.requireNonNull(user, "user");
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !lobbyGameFiles.supportsGameFile(originalFilename)) {
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
            Optional<IGameDefinition> definition =
                lobbyGameFiles.extractUploadedGame(lobbyCode, originalFilename, file);
            if (definition.isEmpty()) {
                return ResponseEntity.badRequest().body("Game definition missing.");
            }

            lobby.loadGame(definition.orElseThrow());
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
