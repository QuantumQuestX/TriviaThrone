package de.quantumquest.triviathrone.game;

import de.quantumquest.triviathrone.dto.game.GameActionMessage;
import de.quantumquest.triviathrone.server.LobbyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class GameWebSocketController {
    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketController.class);
    private final LobbyService lobbyService;
    private final GameEventService gameEventService;

    public GameWebSocketController(LobbyService lobbyService, GameEventService gameEventService) {
        this.lobbyService = lobbyService;
        this.gameEventService = gameEventService;
    }

    @MessageMapping("/game/action")
    public void handleAction(GameActionMessage msg, @AuthenticationPrincipal Principal principal) {
        String username = principal.getName();

        lobbyService.getLobby(msg.lobbyCode()).ifPresent(lobby -> {
            if (!lobby.hasActiveGame()) return;

            GameSession session = lobby.getGameSession();

            switch (msg.action()) {
                case SELECT_QUESTION -> {
                    if (!username.equals(lobby.getHostUsername())) return;
                    session.selectQuestion(msg.roundIdx(), msg.categoryIdx(), msg.questionIdx());
                }
                case OPEN_BUZZ -> {
                    if (!username.equals(lobby.getHostUsername())) return;
                    session.openBuzz();
                }
                case BUZZ -> {
                    session.registerBuzz(username);
                }
                case MARK_CORRECT -> {
                    if (!username.equals(lobby.getHostUsername())) return;
                    session.markCorrect();
                }
                case MARK_INCORRECT -> {
                    if (!username.equals(lobby.getHostUsername())) return;
                    session.markIncorrect();
                }
                case RESET -> {
                    if (!username.equals(lobby.getHostUsername())) return;
                    session.resetToIdle();
                }
            }

            gameEventService.broadcastState(msg.lobbyCode(), session);
        });
    }
}
