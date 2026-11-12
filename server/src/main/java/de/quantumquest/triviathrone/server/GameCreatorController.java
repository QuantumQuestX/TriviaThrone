package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.api.game.GameCreationRequest;
import de.quantumquest.triviathrone.api.game.GameCreatorInfo;
import de.quantumquest.triviathrone.auth.UserAccount;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

@RestController
@RequestMapping("/api/game")
public class GameCreatorController {
    private static final ObjectMapper PLUGIN_JSON_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> JSON_OBJECT_TYPE = new TypeReference<>() {
    };

    private final GameCreationService gameCreationService;

    public GameCreatorController(GameCreationService gameCreationService) {
        this.gameCreationService = Objects.requireNonNull(gameCreationService, "gameCreationService");
    }

    @GetMapping("/creators")
    public List<CreatorResponse> creators() {
        return gameCreationService.creators().stream()
                .map(info -> new CreatorResponse(
                        info.gameType(),
                        new CreatorDescriptorResponse(
                                info.descriptor().title(),
                                info.descriptor().description(),
                                PLUGIN_JSON_MAPPER.convertValue(info.descriptor().editorSchema(), JSON_OBJECT_TYPE)
                        ),
                        gameCreationService.hasCustomEditor(info.gameType())
                ))
                .toList();
    }

    @PostMapping(path = "/create/{gameType}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> create(
            @PathVariable String gameType,
            @RequestBody Map<String, Object> requestBody,
            @AuthenticationPrincipal UserAccount user
    ) throws IOException {
        Objects.requireNonNull(user, "user");
        GameCreationRequest request = parseRequest(requestBody);

        GameCreationService.GeneratedGame game;
        try {
            game = gameCreationService.generate(gameType, request);
        } catch (NoSuchElementException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }

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

    private static GameCreationRequest parseRequest(Map<String, Object> requestBody) {
        Object data = requestBody.get("data");
        if (!(data instanceof Map<?, ?>)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game creation data must be a JSON object.");
        }
        JsonNode jsonData = PLUGIN_JSON_MAPPER.valueToTree(data);
        return new GameCreationRequest(jsonData);
    }

    public record CreatorResponse(
            String gameType,
            CreatorDescriptorResponse descriptor,
            boolean hasCustomEditor
    ) {
    }

    public record CreatorDescriptorResponse(
            String title,
            String description,
            Map<String, Object> editorSchema
    ) {
    }
}
