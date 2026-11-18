package quantumquest.triviathrone.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import quantumquest.triviathrone.api.system.events.GameEndedEvent;
import quantumquest.triviathrone.api.system.events.PlayerJoinedLobbyEvent;
import quantumquest.triviathrone.api.system.events.PlayerLeftLobbyEvent;
import quantumquest.triviathrone.dto.lobby.LobbyCommonMessage;
import quantumquest.triviathrone.mod.SystemModManager;
import quantumquest.triviathrone.server.LobbyEventService;
import quantumquest.triviathrone.server.LobbyGameFiles;
import quantumquest.triviathrone.server.LobbyService;

import java.io.IOException;
import java.security.Principal;
import java.time.Instant;
import java.util.Objects;

/**
 * Handles lobby membership messages sent through STOMP.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Controller
public class WebSocketLobbyController {
    /**
     * Logs events from this component.
     */
    private static final Logger logger = LoggerFactory.getLogger(WebSocketLobbyController.class);
    /**
     * Holds the lobb ervice used by this component.
     */
    private final LobbyService lobbyService;
    /**
     * Holds the lobb ven ervice used by this component.
     */
    private final LobbyEventService lobbyEventService;
    /**
     * Holds the lobb am iles used by this component.
     */
    private final LobbyGameFiles lobbyGameFiles;
    /**
     * Holds the syste ods used by this component.
     */
    private final SystemModManager systemMods;
    private final WebSocketSessionRegistry sessionRegistry;

    /**
     * Creates a WebSocketLobbyController.
     *
     * @param lobbyService      the lobby service
     * @param lobbyEventService the lobby event service
     * @param lobbyGameFiles    the lobby game files
     * @param systemMods        the system-mod manager
     */
    @Autowired
    public WebSocketLobbyController(
        LobbyService lobbyService,
        LobbyEventService lobbyEventService,
        LobbyGameFiles lobbyGameFiles,
        SystemModManager systemMods,
        WebSocketSessionRegistry sessionRegistry
    ) {
        this.lobbyService = Objects.requireNonNull(lobbyService, "lobbyService");
        this.lobbyEventService = Objects.requireNonNull(lobbyEventService, "lobbyEventService");
        this.lobbyGameFiles = Objects.requireNonNull(lobbyGameFiles, "lobbyGameFiles");
        this.systemMods = Objects.requireNonNull(systemMods, "systemMods");
        this.sessionRegistry = Objects.requireNonNull(sessionRegistry, "sessionRegistry");
    }

    public WebSocketLobbyController(
        LobbyService lobbyService,
        LobbyEventService lobbyEventService,
        LobbyGameFiles lobbyGameFiles,
        SystemModManager systemMods
    ) {
        this(
            lobbyService,
            lobbyEventService,
            lobbyGameFiles,
            systemMods,
            new WebSocketSessionRegistry()
        );
    }

    /**
     * Handles a STOMP join message and broadcasts the lobby membership change.
     *
     * @param msg       the msg
     * @param principal the principal
     */
    public void join(LobbyCommonMessage msg, @AuthenticationPrincipal Principal principal) {
        join(msg, principal, null);
    }

    @MessageMapping("/join")
    public void join(LobbyCommonMessage msg, @AuthenticationPrincipal Principal principal, SimpMessageHeaderAccessor headers) {
        Objects.requireNonNull(msg, "msg");
        Objects.requireNonNull(principal, "principal");
        logger.info("OnJoin: {} joined lobby with code {}", principal.getName(), msg.lobbyCode());
        String username = principal.getName();

        lobbyService.getLobby(msg.lobbyCode()).ifPresent(lobby -> {
            boolean joined = lobby.addUser(username);

            if (joined) {
                systemMods.publishEvent(new PlayerJoinedLobbyEvent(Instant.now(), msg.lobbyCode(), username));
            }

            if (headers != null && headers.getSessionId() != null) {
                sessionRegistry.register(
                    headers.getSessionId(),
                    msg.lobbyCode(),
                    username
                );
            }

            lobbyEventService.userJoined(msg.lobbyCode(), lobby.getHostUsername(), username);
        });
    }

    /**
     * Handles a STOMP leave message and removes an empty lobby when necessary.
     *
     * @param msg       the msg
     * @param principal the principal
     */
    @MessageMapping("/leave")
    public void leave(
        LobbyCommonMessage msg,
        @AuthenticationPrincipal Principal principal
    ) {
        Objects.requireNonNull(msg, "msg");
        Objects.requireNonNull(principal, "principal");
        logger.info("OnLeave: {} left lobby with code {}", principal.getName(), msg.lobbyCode());
        String username = principal.getName();

        lobbyService.getLobby(msg.lobbyCode()).ifPresent(lobby -> {
            boolean left = lobby.removeUser(username);
            if (left) {
                sessionRegistry.removeForUser(msg.lobbyCode(), username);
                if (lobby.getGameSession()
                    instanceof quantumquest.triviathrone.api.game.IDisconnectAwareGameSession session) {
                    session.playerDisconnected(username);
                }
                systemMods.publishEvent(new PlayerLeftLobbyEvent(Instant.now(), msg.lobbyCode(), username));
            }
            lobbyEventService.userLeft(msg.lobbyCode(), lobby.getHostUsername(), username);
            if (lobby.listUsernames().isEmpty()) {
                logger.info("Lobby with code {} is empty. Deleting...", msg.lobbyCode());
                if (lobby.hasActiveGame()) {
                    String gameType = lobby.getGameDefinition().gameType();
                    if (lobby.endGameIfActive()) {
                        systemMods.publishEvent(new GameEndedEvent(Instant.now(), msg.lobbyCode(), gameType));
                    }
                }
                lobbyService.removeLobby(lobby.getLobbyCode());
                try {
                    lobbyGameFiles.deleteLobbyFiles(lobby.getLobbyCode());
                } catch (IOException exception) {
                    logger.error("Could not delete uploaded game files for lobby {}", lobby.getLobbyCode(), exception);
                }
            }
        });
    }
}
