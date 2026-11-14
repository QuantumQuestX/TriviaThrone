package quantumquest.triviathrone.server.game;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import quantumquest.triviathrone.dto.game.GameActionMessage;
import quantumquest.triviathrone.server.LobbyService;

import java.security.Principal;
import java.util.Objects;

/**
 * Receives STOMP game actions and forwards them to the owning lobby.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Controller
public class GameWebSocketController {
    /**
     * Holds the lobb ervice used by this component.
     */
    private final LobbyService lobbyService;
    /**
     * Holds the gam essio oordinator used by this component.
     */
    private final GameSessionCoordinator gameSessionCoordinator;

    /**
     * Creates a GameWebSocketController.
     *
     * @param lobbyService           the lobby service
     * @param gameSessionCoordinator the game-session coordinator
     */
    public GameWebSocketController(
            LobbyService lobbyService,
            GameSessionCoordinator gameSessionCoordinator
    ) {
        this.lobbyService = Objects.requireNonNull(lobbyService, "lobbyService");
        this.gameSessionCoordinator = Objects.requireNonNull(gameSessionCoordinator, "gameSessionCoordinator");
    }

    /**
     * Routes a STOMP game action to the matching lobby session.
     *
     * @param msg       the msg
     * @param principal the principal
     */
    @MessageMapping("/game/action")
    public void handleAction(GameActionMessage msg, @AuthenticationPrincipal Principal principal) {
        Objects.requireNonNull(msg, "msg");
        Objects.requireNonNull(principal, "principal");
        String username = principal.getName();

        lobbyService.getLobby(msg.lobbyCode())
                .ifPresent(lobby -> gameSessionCoordinator.handleAction(msg, lobby, username));
    }
}
