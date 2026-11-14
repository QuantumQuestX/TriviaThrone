package quantumquest.triviathrone.server.game.creation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import quantumquest.triviathrone.api.game.GameCreationRequest;
import quantumquest.triviathrone.auth.UserAccount;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Provides HTTP endpoints for listing creators and downloading generated games.
 *
 * @author TiJ
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/game")
public class GameCreatorController {
    /**
     * Serializes and deserializes game-editor JSON.
     */
    private static final ObjectMapper GAME_MOD_JSON_MAPPER = new ObjectMapper();
    /**
     * Stores an internal helper required by this component.
     */
    private static final TypeReference<Map<String, Object>> JSON_OBJECT_TYPE = new TypeReference<>() {
    };

    /**
     * Holds the gam reatio ervice used by this component.
     */
    private final GameCreationService gameCreationService;
    /**
     * Holds the reques arser used by this component.
     */
    private final GameCreationRequestParser requestParser;

    /**
     * Creates a GameCreatorController.
     *
     * @param gameCreationService the game generator
     */
    public GameCreatorController(GameCreationService gameCreationService) {
        this.gameCreationService = Objects.requireNonNull(gameCreationService, "gameCreationService");
        this.requestParser = new GameCreationRequestParser(GAME_MOD_JSON_MAPPER);
    }

    /**
     * Builds a streaming download response for a generated archive.
     *
     * @param game the generated archive to stream
     * @return the archive download response
     */
    private static ResponseEntity<StreamingResponseBody> archiveResponse(
        GameCreationService.GeneratedGame game
    ) {
        StreamingResponseBody body = output -> {
            try (game) {
                Files.copy(game.archive(), output);
            }
        };

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(game.filename()).build().toString()
            )
            .body(body);
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
     * Returns available game creators ordered by game type.
     *
     * @return available game creators ordered by game type
     */
    @GetMapping("/creators")
    public List<CreatorResponse> creators() {
        return gameCreationService.creators().stream()
            .map(info -> new CreatorResponse(
                info.gameType(),
                new CreatorDescriptorResponse(
                    info.descriptor().title(),
                    info.descriptor().description(),
                    GAME_MOD_JSON_MAPPER.convertValue(info.descriptor().editorSchema(), JSON_OBJECT_TYPE)
                ),
                gameCreationService.hasCustomEditor(info.gameType())
            ))
            .toList();
    }

    /**
     * Generates a game archive from a JSON creation request.
     *
     * @param gameType    the game type
     * @param requestBody the request body
     * @param user        the user account
     * @return the archive download response
     * @throws IOException if the requested file operation fails
     */
    @PostMapping(path = "/create/{gameType}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> create(
        @PathVariable String gameType,
        @RequestBody Map<String, Object> requestBody,
        @AuthenticationPrincipal UserAccount user
    ) throws IOException {
        Objects.requireNonNull(user, "user");
        return createArchive(gameType, requestParser.parse(requestBody));
    }

    /**
     * Generates a game archive from a multipart creation request and uploaded assets.
     *
     * @param gameType      the game type
     * @param requestData   the request data
     * @param uploadedFiles the uploaded files
     * @param user          the user account
     * @return the archive download response
     * @throws IOException if the requested file operation fails
     */
    @PostMapping(path = "/create/{gameType}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StreamingResponseBody> createMultipart(
        @PathVariable String gameType,
        @RequestParam("data") String requestData,
        @RequestParam MultiValueMap<String, MultipartFile> uploadedFiles,
        @AuthenticationPrincipal UserAccount user
    ) throws IOException {
        Objects.requireNonNull(user, "user");
        Path uploadDirectory = Files.createTempDirectory("triviathrone-game-assets-");
        GameCreationService.GeneratedGame game;
        try {
            GameCreationRequest request = requestParser.parseMultipart(requestData, uploadedFiles, uploadDirectory);
            game = generate(gameType, request);
        } catch (IOException | RuntimeException exception) {
            try {
                deleteTree(uploadDirectory);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
        try {
            deleteTree(uploadDirectory);
        } catch (IOException cleanupException) {
            try {
                game.close();
            } catch (IOException closeException) {
                cleanupException.addSuppressed(closeException);
            }
            throw cleanupException;
        }
        return archiveResponse(game);
    }

    /**
     * Generates an archive response for the requested game type.
     *
     * @param gameType the selected game type
     * @param request  the validated creation request
     * @return the streaming archive response
     * @throws IOException if archive generation fails
     */
    private ResponseEntity<StreamingResponseBody> createArchive(
        String gameType,
        GameCreationRequest request
    ) throws IOException {
        return archiveResponse(generate(gameType, request));
    }

    /**
     * Generates a game archive and maps request failures to HTTP errors.
     */
    private GameCreationService.GeneratedGame generate(String gameType, GameCreationRequest request) throws IOException {
        GameCreationService.GeneratedGame game;
        try {
            game = gameCreationService.generate(gameType, request);
        } catch (NoSuchElementException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
        return game;
    }

    /**
     * Describes an available game creator.
     *
     * @param gameType        the creator game type
     * @param descriptor      the editor descriptor
     * @param hasCustomEditor whether a custom editor is available
     * @author TiJ
     * @since 2.0.0
     */
    public record CreatorResponse(
        String gameType,
        CreatorDescriptorResponse descriptor,
        boolean hasCustomEditor
    ) {
    }

    /**
     * Describes a game creator's editor.
     *
     * @param title        the editor title
     * @param description  the editor description
     * @param editorSchema the editor JSON schema
     * @author TiJ
     * @since 2.0.0
     */
    public record CreatorDescriptorResponse(
        String title,
        String description,
        Map<String, Object> editorSchema
    ) {
    }
}
