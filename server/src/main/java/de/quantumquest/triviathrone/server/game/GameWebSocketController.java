package de.quantumquest.triviathrone.server.game;

import de.quantumquest.triviathrone.api.game.GameActionResult;
import de.quantumquest.triviathrone.api.game.IGameSession;
import de.quantumquest.triviathrone.dto.game.GameActionMessage;
import de.quantumquest.triviathrone.server.LobbyService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Objects;

@Controller
public class GameWebSocketController {
    private final LobbyService lobbyService;
    private final GameEventService gameEventService;

    public GameWebSocketController(LobbyService lobbyService, GameEventService gameEventService) {
        this.lobbyService = Objects.requireNonNull(lobbyService, "lobbyService");
        this.gameEventService = Objects.requireNonNull(gameEventService, "gameEventService");
    }

    @MessageMapping("/game/action")
    public void handleAction(GameActionMessage msg, @AuthenticationPrincipal Principal principal) {
        Objects.requireNonNull(msg, "msg");
        Objects.requireNonNull(principal, "principal");
        String username = principal.getName();

        lobbyService.getLobby(msg.lobbyCode()).ifPresent(lobby -> {
            if (!lobby.listUsernames().contains(username)) return;

            switch (msg.action()) {
                case "START_GAME" -> {
                    if (!username.equals(lobby.getHostUsername())) return;
                    if (!lobby.hasActiveGame()) lobby.startGame();
                    IGameSession startedSession = lobby.getGameSession();
                    startedSession.setStateChangeListener(
                            () -> gameEventService.broadcastState(msg.lobbyCode(), startedSession)
                    );
                    gameEventService.broadcastState(msg.lobbyCode(), startedSession);
                    return;
                }
                case "END_GAME" -> {
                    if (!username.equals(lobby.getHostUsername())) return;
                    if (!lobby.hasActiveGame()) return;
                    gameEventService.broadcastGameEnded(
                            msg.lobbyCode(),
                            lobby.getGameSession(),
                            lobby.getHostUsername()
                    );
                    lobby.endGame();
                    return;
                }
                case "SYNC" -> {
                    if (lobby.hasActiveGame()) {
                        gameEventService.broadcastState(msg.lobbyCode(), lobby.getGameSession());
                    }
                    return;
                }
                default -> {
                    if (!lobby.hasActiveGame()) return;
                    IGameSession session = lobby.getGameSession();
                    boolean isHost = username.equals(lobby.getHostUsername());
                    GameActionResult result = session.handleAction(msg, username, isHost);
                    if (!result.accepted()) return;
                    if (result.gameEnded()) {
                        gameEventService.broadcastGameEnded(msg.lobbyCode(), session, lobby.getHostUsername());
                        lobby.endGame();
                        return;
                    }
                    gameEventService.broadcastState(msg.lobbyCode(), session);
                }
            }
        });
    }
}
