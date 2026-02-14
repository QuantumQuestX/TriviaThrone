package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.auth.UserAccount;
import de.quantumquest.triviathrone.dto.lobby.LobbyMessage;
import de.quantumquest.triviathrone.dto.lobby.LobbyDiscoveryMessage;
import de.quantumquest.triviathrone.dto.lobby.LobbyInfoMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/lobbies")
public class LobbyController {
    private static final Logger logger = LoggerFactory.getLogger(LobbyController.class);
    private final LobbyService lobbyService;

    public LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @GetMapping
    public List<LobbyDiscoveryMessage> listLobbies() {
        logger.info("GET: listing lobbies");
        return lobbyService.listLobbies().stream()
                .map(lobby -> new LobbyDiscoveryMessage(lobby.getLobbyName(), lobby.getLobbyCode()))
                .toList();
    }

    @GetMapping("/info")
    public LobbyInfoMessage getLobbyInformation(@RequestParam String code) {
        logger.info("GET: getting information of lobby with code {}", code);
        if (code == null || code.isBlank()) return null;
        Optional<Lobby> optLobby = lobbyService.getLobby(code);
        return optLobby.map(lobby ->
                            new LobbyInfoMessage(code, lobby.getHostUsername(), lobby.listUsernames()))
                       .orElse(null);
    }

    @PostMapping
    public boolean isLobbyValid(@RequestParam String code) {
        logger.info("POST: checking lobby with code {} exists", code);
        if (code == null || code.isBlank())
            return false;
        return lobbyService.getLobby(code).isPresent();
    }

    @PostMapping("/create")
    public LobbyMessage createLobby(@RequestParam String name, @AuthenticationPrincipal UserAccount user) {
        logger.info("POST: creating lobby with name {} by user {}", name, user);
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Lobby name cannot be empty");
        Lobby lobby = lobbyService.createLobby(name, user);
        return new LobbyMessage(lobby.getLobbyCode());
    }
}
