package quantumquest.triviathrone.server;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;
import quantumquest.triviathrone.api.game.GameUiDescriptor;
import quantumquest.triviathrone.api.game.IGameDefinition;
import quantumquest.triviathrone.api.game.IGameProvider;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Serves game-mod editor, player, and UI asset resources.
 *
 * @author TiJ
 * @since 2.0.0
 */
@RestController
public class GameModUiController {
    /**
     * Stores an internal helper required by this component.
     */
    private static final String UI_RESOURCE_ROOT = "ui/";
    /**
     * Holds the safe_resource_path used by this component.
     */
    private static final Pattern SAFE_RESOURCE_PATH =
            Pattern.compile("[A-Za-z0-9_-][A-Za-z0-9._-]*(?:/[A-Za-z0-9_-][A-Za-z0-9._-]*)*");

    /**
     * Holds the gam roviders used by this component.
     */
    private final GameProviderRegistry gameProviders;
    /**
     * Holds the lobb ervice used by this component.
     */
    private final LobbyService lobbyService;

    /**
     * Creates a GameModUiController.
     *
     * @param gameProviders the game-provider registry
     * @param lobbyService  the lobby service
     */
    public GameModUiController(GameProviderRegistry gameProviders, LobbyService lobbyService) {
        this.gameProviders = Objects.requireNonNull(gameProviders, "gameProviders");
        this.lobbyService = Objects.requireNonNull(lobbyService, "lobbyService");
    }

    /**
     * Returns a redirect response for the given location.
     */
    private static ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(HttpStatus.FOUND)
            .header("Location", location)
            .build();
    }

    /**
     * Validates and normalizes a UI resource path.
     */
    private static String normalizeResourcePath(String requestPath) {
        if (requestPath == null || requestPath.isBlank() || requestPath.contains("\\")) {
            return null;
        }

        String path = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
        if (path.isBlank() || path.startsWith("/") || path.contains("//")
                || !SAFE_RESOURCE_PATH.matcher(path).matches()) {
            return null;
        }
        for (String segment : path.split("/")) {
            if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
                return null;
            }
        }
        return path;
    }

    /**
     * Redirects the legacy editor route to the game-creator view.
     *
     * @return the generated result
     */
    @GetMapping("/editor")
    public ResponseEntity<Void> defaultEditor() {
        return redirect("/game-creator");
    }

    /**
     * Redirects a lobby player to its loaded game's player page.
     *
     * @param lobbyCode the lobby code
     * @return the generated result
     */
    @GetMapping("/game")
    public ResponseEntity<Void> lobbyGame(@RequestParam("code") String lobbyCode) {
        Lobby lobby = lobbyService.getLobby(lobbyCode)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Lobby not found."));
        IGameDefinition definition = lobby.getGameDefinition();
        if (definition == null) {
            throw new ResponseStatusException(NOT_FOUND, "No game is loaded in this lobby.");
        }
        String gameType = definition.gameType();
        requireUiProvider(gameType);
        String location = "/games/" + UriUtils.encodePathSegment(gameType, StandardCharsets.UTF_8)
                + "/play?code=" + UriUtils.encodeQueryParam(lobbyCode, StandardCharsets.UTF_8);
        return redirect(location);
    }

    /**
     * Serves the requested game type's editor page.
     *
     * @param gameType the game type
     * @return the requested editor page
     */
    @GetMapping(path = "/games/{gameType}/editor", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> editor(@PathVariable String gameType) {
        return htmlPage(gameType, true);
    }

    /**
     * Serves the requested game type's player page.
     *
     * @param gameType the game type
     * @return the requested game page
     */
    @GetMapping(path = "/games/{gameType}/play", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> game(@PathVariable String gameType) {
        return htmlPage(gameType, false);
    }

    /**
     * Serves a safe UI asset from the requested game mod.
     *
     * @param gameType  the game type
     * @param assetPath the asset path
     * @return the requested UI resource
     */
    @GetMapping({"/mods/{gameType}/ui/{*assetPath}", "/plugins/{gameType}/ui/{*assetPath}"})
    public ResponseEntity<Resource> asset(
            @PathVariable String gameType,
            @PathVariable String assetPath
    ) {
        Resource resource = modResource(gameType, assetPath)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Game mod asset not found."));
        MediaType contentType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
            .contentType(contentType)
            .cacheControl(CacheControl.noCache())
            .body(resource);
    }

    /**
     * Returns the requested game UI HTML page.
     */
    private ResponseEntity<Resource> htmlPage(String gameType, boolean editor) {
        IGameProvider provider = requireUiProvider(gameType);
        GameUiDescriptor descriptor = provider.uiDescriptor().orElseThrow();
        String pagePath = editor ? descriptor.editorPage() : descriptor.gamePage();
        Resource page = modResource(provider, pagePath)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Game mod page not found."));
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .cacheControl(CacheControl.noCache())
            .body(page);
    }

    /**
     * Finds a game provider that exposes a UI.
     */
    private IGameProvider requireUiProvider(String gameType) {
        IGameProvider provider = gameProviders.providerForType(gameType)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Game mod not found."));
        if (provider.uiDescriptor().isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "Game mod has no UI.");
        }
        return provider;
    }

    /**
     * Finds a safe UI resource for a game type or provider.
     */
    private Optional<Resource> modResource(String gameType, String requestPath) {
        return gameProviders.providerForType(gameType)
            .flatMap(provider -> modResource(provider, requestPath));
    }

    /**
     * Finds a safe UI resource for a game type or provider.
     */
    private Optional<Resource> modResource(IGameProvider provider, String requestPath) {
        String relativePath = normalizeResourcePath(requestPath);
        if (relativePath == null) {
            return Optional.empty();
        }

        ClassLoader modClassLoader = provider.getClass().getClassLoader();
        URL resourceUrl = modClassLoader.getResource(UI_RESOURCE_ROOT + relativePath);
        if (resourceUrl == null) {
            return Optional.empty();
        }

        Resource resource = new UrlResource(resourceUrl);
        return resource.exists() && resource.isReadable()
            ? Optional.of(resource)
            : Optional.empty();
    }
}
