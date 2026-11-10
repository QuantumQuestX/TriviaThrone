package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.auth.UserAccount;
import de.quantumquest.triviathrone.dto.lobby.LobbyCommonMessage;
import de.quantumquest.triviathrone.dto.lobby.LobbyInfoMessage;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/lobbies")
public class LobbyController {
    private static final Logger logger = LoggerFactory.getLogger(LobbyController.class);
    private final LobbyService lobbyService;

    public LobbyController(LobbyService lobbyService) {
        this.lobbyService = Objects.requireNonNull(lobbyService, "lobbyService");
    }

    @GetMapping
    public List<LobbyCommonMessage> listLobbies() {
        logger.debug("Listing lobbies");
        return lobbyService.listLobbies().stream()
                .map(lobby -> new LobbyCommonMessage(lobby.getLobbyName(), lobby.getLobbyCode()))
                .toList();
    }

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

    @GetMapping("/exists")
    public boolean isLobbyValid(@RequestParam("code") String code) {
        logger.debug("Checking whether lobby {} exists", code);
        if (code.isBlank()) {
            return false;
        }
        return lobbyService.getLobby(code).isPresent();
    }

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
