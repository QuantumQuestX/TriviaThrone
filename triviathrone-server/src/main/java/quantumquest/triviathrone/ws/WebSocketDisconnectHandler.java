package quantumquest.triviathrone.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import quantumquest.triviathrone.api.game.IDisconnectAwareGameSession;
import quantumquest.triviathrone.api.system.events.PlayerDisconnectedEvent;
import quantumquest.triviathrone.mod.SystemModManager;
import quantumquest.triviathrone.server.LobbyEventService;
import quantumquest.triviathrone.server.LobbyGameFiles;
import quantumquest.triviathrone.server.LobbyService;

import java.io.IOException;
import java.time.Instant;

/**
 * Handles WebSocket disconnect events and updates lobby membership.
 *
 * @author TiJ
 * @since 2.2.0
 */
@Component
public final class WebSocketDisconnectHandler {
    /**
     * Logs disconnect cleanup events.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(WebSocketDisconnectHandler.class);
    /**
     * Tracks WebSocket sessions and their lobby participants.
     */
    private final WebSocketSessionRegistry sessionRegistry;
    /**
     * Provides access to active lobbies.
     */
    private final LobbyService lobbyService;
    /**
     * Publishes system events.
     */
    private final SystemModManager systemMods;
    /**
     * Publishes lobby membership changes.
     */
    private final LobbyEventService lobbyEventService;
    /**
     * Removes uploaded files when a lobby becomes empty.
     */
    private final LobbyGameFiles lobbyGameFiles;

    /**
     * Creates a WebSocketDisconnectHandler.
     *
     * @param sessionRegistry   the WebSocket session registry
     * @param lobbyService      the lobby service
     * @param systemMods        the system-mod manager
     * @param lobbyEventService the lobby event publisher
     * @param lobbyGameFiles    the lobby game-file manager
     */
    public WebSocketDisconnectHandler(
        WebSocketSessionRegistry sessionRegistry,
        LobbyService lobbyService,
        SystemModManager systemMods,
        LobbyEventService lobbyEventService,
        LobbyGameFiles lobbyGameFiles
    ) {
        this.sessionRegistry = sessionRegistry;
        this.lobbyService = lobbyService;
        this.systemMods = systemMods;
        this.lobbyEventService = lobbyEventService;
        this.lobbyGameFiles = lobbyGameFiles;
    }

    /**
     * Removes the disconnected participant from its lobby and notifies interested components.
     *
     * <p>This emits a {@link PlayerDisconnectedEvent}, invokes
     * {@link IDisconnectAwareGameSession} when supported, and removes empty lobbies.</p>
     *
     * @param event the WebSocket disconnect event
     */
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = headers.getSessionId();

        if (sessionId == null) {
            return;
        }

        WebSocketSessionRegistry.Connection connection = sessionRegistry.remove(sessionId);

        if (connection == null) {
            return;
        }

        lobbyService.getLobby(connection.lobbyCode()).ifPresent(lobby -> {
            boolean removed = lobby.removeUser(connection.username());

            if (!removed) {
                return;
            }

            lobbyEventService.userLeft(
                connection.lobbyCode(),
                lobby.getHostUsername(),
                connection.username()
            );

            if (lobby.getGameSession() instanceof IDisconnectAwareGameSession session) {
                session.playerDisconnected(connection.username());
            }

            systemMods.publishEvent(
                new PlayerDisconnectedEvent(
                    Instant.now(),
                    connection.lobbyCode(),
                    connection.username()
                )
            );

            if (lobby.listUsernames().isEmpty()) {
                lobbyService.removeLobby(lobby.getLobbyCode());
                try {
                    lobbyGameFiles.deleteLobbyFiles(lobby.getLobbyCode());
                } catch (IOException exception) {
                    logger.error(
                        "Could not delete uploaded game files for lobby {}",
                        lobby.getLobbyCode(),
                        exception
                    );
                }
            }
        });
    }
}
