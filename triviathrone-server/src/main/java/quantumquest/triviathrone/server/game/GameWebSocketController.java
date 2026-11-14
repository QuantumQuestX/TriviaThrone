package quantumquest.triviathrone.server.game;

import quantumquest.triviathrone.dto.game.GameActionMessage;
import quantumquest.triviathrone.server.LobbyService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Objects;

@Controller
public class GameWebSocketController {
    private final LobbyService lobbyService;
    private final GameSessionCoordinator gameSessionCoordinator;

    public GameWebSocketController(
            LobbyService lobbyService,
            GameSessionCoordinator gameSessionCoordinator
    ) {
        this.lobbyService = Objects.requireNonNull(lobbyService, "lobbyService");
        this.gameSessionCoordinator = Objects.requireNonNull(gameSessionCoordinator, "gameSessionCoordinator");
    }

    @MessageMapping("/game/action")
    public void handleAction(GameActionMessage msg, @AuthenticationPrincipal Principal principal) {
        Objects.requireNonNull(msg, "msg");
        Objects.requireNonNull(principal, "principal");
        String username = principal.getName();

        lobbyService.getLobby(msg.lobbyCode())
                .ifPresent(lobby -> gameSessionCoordinator.handleAction(msg, lobby, username));
    }
}
