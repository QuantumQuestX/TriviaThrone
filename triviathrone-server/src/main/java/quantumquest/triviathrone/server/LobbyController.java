package quantumquest.triviathrone.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import quantumquest.triviathrone.auth.UserAccount;
import quantumquest.triviathrone.dto.lobby.LobbyCommonMessage;
import quantumquest.triviathrone.dto.lobby.LobbyInfoMessage;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides HTTP endpoints for discovering and creating lobbies.
 *
 * @author TiJ
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/lobbies")
public class LobbyController {
    /**
     * Logs events from this component.
     */
    private static final Logger logger = LoggerFactory.getLogger(LobbyController.class);
    /**
     * Holds the lobb ervice used by this component.
     */
    private final LobbyService lobbyService;

    /**
     * Creates a LobbyController.
     *
     * @param lobbyService the lobby service
     */
    public LobbyController(LobbyService lobbyService) {
        this.lobbyService = Objects.requireNonNull(lobbyService, "lobbyService");
    }

    /**
     * Returns the currently active lobbies.
     *
     * @return the currently active lobbies
     */
    @GetMapping
    public List<LobbyCommonMessage> listLobbies() {
        logger.debug("Listing lobbies");
        return lobbyService.listLobbies().stream()
            .map(lobby -> new LobbyCommonMessage(lobby.getLobbyName(), lobby.getLobbyCode()))
            .toList();
    }

    /**
     * Returns the current membership and game status for a lobby.
     *
     * @param code lobby code to inspect
     * @return the lobby information, or {@code null} if no such lobby exists
     */
    @GetMapping("/info")
    public LobbyInfoMessage getLobbyInformation(@RequestParam("code") String code) {
        logger.debug("Getting information for lobby {}", code);
        if (code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lobby code cannot be empty");
        }

        Optional<Lobby> optLobby = lobbyService.getLobby(code);
        return optLobby.map(lobby ->
                new LobbyInfoMessage(
                    code,
                    lobby.getHostUsername(),
                    lobby.listUsernames(),
                    lobby.hasLoadedGame(),
                    lobby.hasActiveGame()
                ))
            .orElse(null);
    }

    /**
     * Reports whether lobby valid.
     */
    @GetMapping("/exists")
    public boolean isLobbyValid(@RequestParam("code") String code) {
        logger.debug("Checking whether lobby {} exists", code);
        if (code.isBlank()) {
            return false;
        }
        return lobbyService.getLobby(code).isPresent();
    }

    /**
     * Creates a lobby hosted by the authenticated user.
     *
     * @param name the lobby name
     * @param user the user account
     * @return the created a lobby hosted by the authenticated user
     */
    @PostMapping("/create")
    public LobbyCommonMessage createLobby(
            @RequestParam("name") String name,
            @AuthenticationPrincipal UserAccount user
    ) {
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lobby name cannot be empty");
        }

        logger.info("Creating lobby with name {} for user {}", name, user.getUsername());
        Lobby lobby = lobbyService.createLobby(name, user);
        return new LobbyCommonMessage(lobby.getLobbyName(), lobby.getLobbyCode());
    }
}
