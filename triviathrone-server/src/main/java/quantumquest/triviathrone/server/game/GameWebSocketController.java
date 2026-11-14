package quantumquest.triviathrone.server.game;

import quantumquest.triviathrone.api.game.GameActionResult;
import quantumquest.triviathrone.api.game.GameControlAction;
import quantumquest.triviathrone.api.game.IGameSession;
import quantumquest.triviathrone.api.system.events.GameEndedEvent;
import quantumquest.triviathrone.api.system.events.GameStartedEvent;
import quantumquest.triviathrone.dto.game.GameActionMessage;
import quantumquest.triviathrone.mod.SystemModManager;
import quantumquest.triviathrone.server.Lobby;
import quantumquest.triviathrone.server.LobbyService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.Objects;

@Controller
public class GameWebSocketController {
    private final LobbyService lobbyService;
    private final GameEventService gameEventService;
    private final SystemModManager systemMods;

    public GameWebSocketController(
            LobbyService lobbyService,
            GameEventService gameEventService,
            SystemModManager systemMods
    ) {
        this.lobbyService = Objects.requireNonNull(lobbyService, "lobbyService");
        this.gameEventService = Objects.requireNonNull(gameEventService, "gameEventService");
        this.systemMods = Objects.requireNonNull(systemMods, "systemMods");
    }

    @MessageMapping("/game/action")
    public void handleAction(GameActionMessage msg, @AuthenticationPrincipal Principal principal) {
        Objects.requireNonNull(msg, "msg");
        Objects.requireNonNull(principal, "principal");
        String username = principal.getName();

        lobbyService.getLobby(msg.lobbyCode()).ifPresent(lobby -> {
            if (!lobby.listUsernames().contains(username)) return;

            var controlAction = GameControlAction.fromWireName(msg.action());
            if (controlAction.isPresent()) {
                if (!msg.parameters().isEmpty()) return;
                handleControlAction(controlAction.get(), msg, lobby, username);
                return;
            }
            handleGameAction(msg, lobby, username);
        });
    }

    private void handleControlAction(
            GameControlAction action,
            GameActionMessage message,
            Lobby lobby,
            String username
    ) {
        switch (action) {
            case START_GAME -> {
                if (!username.equals(lobby.getHostUsername())) return;
                boolean started = lobby.startGameIfInactive();
                IGameSession session = lobby.getGameSession();
                if (started) {
                    systemMods.publishEvent(new GameStartedEvent(
                            Instant.now(), message.lobbyCode(), lobby.getGameDefinition().gameType()
                    ));
                }
                session.setStateChangeListener(
                        () -> gameEventService.broadcastState(message.lobbyCode(), session)
                );
                gameEventService.broadcastState(message.lobbyCode(), session);
            }
            case END_GAME -> {
                if (!username.equals(lobby.getHostUsername()) || !lobby.hasActiveGame()) return;
                String gameType = lobby.getGameDefinition().gameType();
                IGameSession session = lobby.getGameSession();
                gameEventService.broadcastGameEnded(message.lobbyCode(), session, lobby.getHostUsername());
                if (lobby.endGameIfActive()) {
                    systemMods.publishEvent(new GameEndedEvent(Instant.now(), message.lobbyCode(), gameType));
                }
            }
            case SYNC -> {
                if (lobby.hasActiveGame()) {
                    gameEventService.broadcastState(message.lobbyCode(), lobby.getGameSession());
                }
            }
        }
    }

    private void handleGameAction(GameActionMessage message, Lobby lobby, String username) {
        if (!lobby.hasActiveGame()) return;
        IGameSession session = lobby.getGameSession();
        boolean isHost = username.equals(lobby.getHostUsername());
        GameActionResult result = session.handleAction(message, username, isHost);
        if (!result.accepted()) return;
        if (result.gameEnded()) {
            String gameType = lobby.getGameDefinition().gameType();
            gameEventService.broadcastGameEnded(message.lobbyCode(), session, lobby.getHostUsername());
            if (lobby.endGameIfActive()) {
                systemMods.publishEvent(new GameEndedEvent(Instant.now(), message.lobbyCode(), gameType));
            }
            return;
        }
        gameEventService.broadcastState(message.lobbyCode(), session);
    }
}
