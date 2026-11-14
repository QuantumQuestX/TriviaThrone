package quantumquest.triviathrone.server.game;

import org.springframework.stereotype.Service;
import quantumquest.triviathrone.api.game.GameActionResult;
import quantumquest.triviathrone.api.game.GameControlAction;
import quantumquest.triviathrone.api.game.IGameSession;
import quantumquest.triviathrone.api.system.events.GameEndedEvent;
import quantumquest.triviathrone.api.system.events.GameStartedEvent;
import quantumquest.triviathrone.dto.game.GameActionMessage;
import quantumquest.triviathrone.mod.SystemModManager;
import quantumquest.triviathrone.server.Lobby;

import java.time.Instant;
import java.util.Objects;

/**
 * Coordinates authorized game control and gameplay actions for a lobby.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Service
public class GameSessionCoordinator {
    /**
     * Holds the gam ven ervice used by this component.
     */
    private final GameEventService gameEventService;
    /**
     * Holds the syste ods used by this component.
     */
    private final SystemModManager systemMods;

    /**
     * Creates a GameSessionCoordinator.
     *
     * @param gameEventService the game event publisher
     * @param systemMods       the system-mod manager
     */
    public GameSessionCoordinator(GameEventService gameEventService, SystemModManager systemMods) {
        this.gameEventService = Objects.requireNonNull(gameEventService, "gameEventService");
        this.systemMods = Objects.requireNonNull(systemMods, "systemMods");
    }

    /**
     * Routes a STOMP game action to the matching lobby session.
     *
     * @param message  the STOMP message
     * @param lobby    the target lobby
     * @param username the account username
     */
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

    /**
     * Dispatches a start, end, or synchronization action for a lobby game.
     *
     * @param action   the parsed control action
     * @param message  the requested game action
     * @param lobby    the lobby whose game is controlled
     * @param username the requesting username
     */
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

    /**
     * Starts the selected game session when the caller is allowed.
     */
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

    /**
     * Ends the current game session when the caller is allowed.
     */
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

    /**
     * Publishes the current game state to its lobby.
     */
    private void syncGame(Lobby lobby) {
        IGameSession session = lobby.getGameSession();
        if (session != null) {
            broadcastStateIfCurrent(lobby, session);
        }
    }

    /**
     * Applies a game action when the caller is allowed.
     */
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

    /**
     * Publishes state only while the session remains current.
     */
    private void broadcastStateIfCurrent(Lobby lobby, IGameSession session) {
        if (lobby.getGameSession() == session) {
            gameEventService.broadcastState(lobby.getLobbyCode(), session);
        }
    }
}
