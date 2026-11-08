package de.quantumquest.triviathrone.game;

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

            if (msg.action() != GameActionMessage.ActionType.START_GAME && !lobby.hasActiveGame()) return;

            GameSession session;

            switch (msg.action()) {
                case START_GAME -> {
                    if (!username.equals(lobby.getHostUsername())) return;
                    if (!lobby.hasActiveGame()) lobby.startGame();
                    GameSession startedSession = lobby.getGameSession();
                    startedSession.setStateChangeListener(
                            () -> gameEventService.broadcastState(msg.lobbyCode(), startedSession)
                    );
                    gameEventService.broadcastState(msg.lobbyCode(), startedSession);
                    return;
                }
                case END_GAME -> {
                    if (!username.equals(lobby.getHostUsername())) return;
                    gameEventService.broadcastGameEnded(
                            msg.lobbyCode(),
                            lobby.getGameSession(),
                            lobby.getHostUsername()
                    );
                    lobby.endGame();
                    return;
                }
                case SYNC -> session = lobby.getGameSession();
                case SELECT_ROUND -> {
                    if (!username.equals(lobby.getHostUsername())) return;
                    session = lobby.getGameSession();
                    session.selectRound(msg.roundIdx());
                }
                case SELECT_QUESTION -> {
                    if (!username.equals(lobby.getHostUsername())) return;
                    session = lobby.getGameSession();
                    session.selectQuestion(msg.roundIdx(), msg.categoryIdx(), msg.questionIdx());
                }
                case PLAY_MEDIA -> {
                    if (!username.equals(lobby.getHostUsername())) return;
                    session = lobby.getGameSession();
                    session.playMedia();
                }
                case OPEN_BUZZ -> {
                    if (!username.equals(lobby.getHostUsername())) return;
                    session = lobby.getGameSession();
                    session.openBuzz();
                }
                case CLOSE_BUZZ -> {
                    if (!username.equals(lobby.getHostUsername())) return;
                    session = lobby.getGameSession();
                    session.closeBuzz();
                }
                case BUZZ -> {
                    if (username.equals(lobby.getHostUsername())) return;
                    session = lobby.getGameSession();
                    session.registerBuzz(username);
                }
                case MARK_CORRECT -> {
                    if (!username.equals(lobby.getHostUsername())) return;
                    session = lobby.getGameSession();
                    session.markCorrect();
                }
                case MARK_INCORRECT -> {
                    if (!username.equals(lobby.getHostUsername())) return;
                    session = lobby.getGameSession();
                    session.markIncorrect();
                }
                case RESET -> {
                    if (!username.equals(lobby.getHostUsername())) return;
                    session = lobby.getGameSession();
                    if (!session.hasUnusedQuestions()) {
                        gameEventService.broadcastGameEnded(
                                msg.lobbyCode(),
                                session,
                                lobby.getHostUsername()
                        );
                        lobby.endGame();
                        return;
                    }
                    session.resetToIdle();
                }
                default -> {
                    return;
                }
            }

            gameEventService.broadcastState(msg.lobbyCode(), session);
        });
    }
}
