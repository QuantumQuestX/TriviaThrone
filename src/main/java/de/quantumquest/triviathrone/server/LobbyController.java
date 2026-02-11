package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.auth.UserAccount;
import de.quantumquest.triviathrone.auth.UserConfigLoader;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lobbies")
public class LobbyController {
    private final LobbyService lobbyService;
    private final UserConfigLoader userLoader;

    public LobbyController(LobbyService lobbyService, UserConfigLoader loader) {
        this.lobbyService = lobbyService;
        this.userLoader = loader;
    }

    @GetMapping
    public List<Lobby> list() {
        return lobbyService.listLobbies();
    }

    @PostMapping("/create")
    public Lobby create(@RequestParam String name) {
        return lobbyService.createLobby(name);
    }

    @PostMapping("/join")
    public Lobby join(@RequestParam String code, @AuthenticationPrincipal UserAccount authUser) {
        return lobbyService.joinLobby(code, authUser);
    }

    @PostMapping("/leave")
    public void leave(@RequestParam String code, @AuthenticationPrincipal UserAccount authUser) {
        lobbyService.leaveLobby(code, authUser);
    }
}
