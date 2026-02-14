package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.auth.UserAccount;
import de.quantumquest.triviathrone.dto.LobbyMessage;
import de.quantumquest.triviathrone.dto.LobbyDiscoveryMessage;
import de.quantumquest.triviathrone.dto.LobbyInfoMessage;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/lobbies")
public class LobbyController {
    private final LobbyService lobbyService;

    public LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @GetMapping
    public List<LobbyDiscoveryMessage> listLobbies() {
        return lobbyService.listLobbies().stream()
                .map(lobby -> new LobbyDiscoveryMessage(lobby.getName(), lobby.getCode()))
                .toList();
    }

    @GetMapping("/info")
    public LobbyInfoMessage getLobbyInformation(@RequestParam String code) {
        if (code == null || code.isBlank()) return null;
        Optional<Lobby> optLobby = lobbyService.getLobby(code);
        return optLobby.map(lobby ->
                            new LobbyInfoMessage(code, lobby.listUsernames(), lobby.getHostUser()))
                       .orElse(null);
    }

    @PostMapping
    public boolean isLobbyValid(@RequestParam String code) {
        if (code == null || code.isBlank())
            return false;
        return lobbyService.getLobby(code).isPresent();
    }

    @PostMapping("/create")
    public LobbyMessage createLobby(@RequestParam String name, @AuthenticationPrincipal UserAccount user) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Lobby name cannot be empty");
        Lobby lobby = lobbyService.createLobby(name, user);
        return new LobbyMessage(lobby.getCode());
    }
}
