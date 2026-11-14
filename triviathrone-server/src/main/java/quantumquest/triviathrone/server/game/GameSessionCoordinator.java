package quantumquest.triviathrone.server.game;

import quantumquest.triviathrone.api.game.GameActionResult;
import quantumquest.triviathrone.api.game.GameControlAction;
import quantumquest.triviathrone.api.game.IGameSession;
import quantumquest.triviathrone.api.system.events.GameEndedEvent;
import quantumquest.triviathrone.api.system.events.GameStartedEvent;
import quantumquest.triviathrone.dto.game.GameActionMessage;
import quantumquest.triviathrone.mod.SystemModManager;
import quantumquest.triviathrone.server.Lobby;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

@Service
public class GameSessionCoordinator {
    private final GameEventService gameEventService;
    private final SystemModManager systemMods;

    public GameSessionCoordinator(GameEventService gameEventService, SystemModManager systemMods) {
        this.gameEventService = Objects.requireNonNull(gameEventService, "gameEventService");
        this.systemMods = Objects.requireNonNull(systemMods, "systemMods");
    }

    public void handleAction(GameActionMessage message, Lobby lobby, String username) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(lobby, "lobby");
        Objects.requireNonNull(username, "username");
        if (!lobby.listUsernames().contains(username)) {
            return;
        }

        var controlAction = GameControlAction.fromWireName(message.action());
        if (controlAction.isPresent()) {
            if (message.parameters().isEmpty()) {
                handleControlAction(controlAction.get(), message, lobby, username);
            }
            return;
        }
        handleGameAction(message, lobby, username);
    }

    private void handleControlAction(
            GameControlAction action,
            GameActionMessage message,
            Lobby lobby,
            String username
    ) {
        switch (action) {
            case START_GAME -> startGame(message, lobby, username);
            case END_GAME -> endGame(message, lobby, username);
            case SYNC -> syncGame(lobby);
        }
    }

    private void startGame(GameActionMessage message, Lobby lobby, String username) {
        if (!username.equals(lobby.getHostUsername())) {
            return;
        }
        boolean started = lobby.startGameIfInactive();
        IGameSession session = lobby.getGameSession();
        if (started) {
            systemMods.publishEvent(new GameStartedEvent(
                    Instant.now(), message.lobbyCode(), lobby.getGameDefinition().gameType()
            ));
        }
        session.setStateChangeListener(() -> broadcastStateIfCurrent(lobby, session));
        broadcastStateIfCurrent(lobby, session);
    }

    private void endGame(GameActionMessage message, Lobby lobby, String username) {
        if (!username.equals(lobby.getHostUsername()) || !lobby.hasActiveGame()) {
            return;
        }
        String gameType = lobby.getGameDefinition().gameType();
        IGameSession session = lobby.getGameSession();
        if (session == null) {
            return;
        }

        gameEventService.broadcastGameEnded(message.lobbyCode(), session, lobby.getHostUsername());
        if (lobby.endGameIfActive(session)) {
            systemMods.publishEvent(new GameEndedEvent(Instant.now(), message.lobbyCode(), gameType));
        }
    }

    private void syncGame(Lobby lobby) {
        IGameSession session = lobby.getGameSession();
        if (session != null) {
            broadcastStateIfCurrent(lobby, session);
        }
    }

    private void handleGameAction(GameActionMessage message, Lobby lobby, String username) {
        IGameSession session = lobby.getGameSession();
        if (session == null) {
            return;
        }

        boolean isHost = username.equals(lobby.getHostUsername());
        GameActionResult result = session.handleAction(message, username, isHost);
        if (lobby.getGameSession() != session) {
            return;
        }
        if (!result.accepted()) {
            return;
        }
        if (result.gameEnded()) {
            String gameType = lobby.getGameDefinition().gameType();
            gameEventService.broadcastGameEnded(message.lobbyCode(), session, lobby.getHostUsername());
            if (lobby.endGameIfActive(session)) {
                systemMods.publishEvent(new GameEndedEvent(Instant.now(), message.lobbyCode(), gameType));
            }
            return;
        }
        broadcastStateIfCurrent(lobby, session);
    }

    private void broadcastStateIfCurrent(Lobby lobby, IGameSession session) {
        if (lobby.getGameSession() == session) {
            gameEventService.broadcastState(lobby.getLobbyCode(), session);
        }
    }
}
