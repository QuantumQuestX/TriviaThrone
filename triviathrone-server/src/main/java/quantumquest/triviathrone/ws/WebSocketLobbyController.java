package quantumquest.triviathrone.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import quantumquest.triviathrone.api.system.events.PlayerJoinedLobbyEvent;
import quantumquest.triviathrone.api.system.events.PlayerLeftLobbyEvent;
import quantumquest.triviathrone.dto.lobby.LobbyCommonMessage;
import quantumquest.triviathrone.dto.lobby.LobbyJoinMessage;
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
    /**
     * Tracks WebSocket sessions so disconnects can update lobby membership.
     */
    private final WebSocketSessionRegistry sessionRegistry;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a WebSocketLobbyController.
     *
     * @param lobbyService      the lobby service
     * @param lobbyEventService the lobby event service
     * @param lobbyGameFiles    the lobby game files
     * @param systemMods        the system-mod manager
     * @param sessionRegistry   the WebSocket session registry
     */
    @Autowired
    public WebSocketLobbyController(
        LobbyService lobbyService,
        LobbyEventService lobbyEventService,
        LobbyGameFiles lobbyGameFiles,
        SystemModManager systemMods,
        WebSocketSessionRegistry sessionRegistry,
        PasswordEncoder passwordEncoder
    ) {
        this.lobbyService = Objects.requireNonNull(lobbyService, "lobbyService");
        this.lobbyEventService = Objects.requireNonNull(lobbyEventService, "lobbyEventService");
        this.lobbyGameFiles = Objects.requireNonNull(lobbyGameFiles, "lobbyGameFiles");
        this.systemMods = Objects.requireNonNull(systemMods, "systemMods");
        this.sessionRegistry = Objects.requireNonNull(sessionRegistry, "sessionRegistry");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
    }

    /**
     * Creates a controller with a local WebSocket session registry.
     *
     * <p>This overload is retained for callers that construct the controller directly.</p>
     *
     * @param lobbyService      the lobby service
     * @param lobbyEventService the lobby event service
     * @param lobbyGameFiles    the lobby game files
     * @param systemMods        the system-mod manager
     */
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
            new WebSocketSessionRegistry(),
            new BCryptPasswordEncoder()
        );
    }

    /**
     * Handles a STOMP join message and broadcasts the lobby membership change.
     *
     * @param msg       the join message
     * @param principal the authenticated participant
     */
    public void join(LobbyCommonMessage msg, @AuthenticationPrincipal Principal principal) {
        join(new LobbyJoinMessage(msg.name(), msg.lobbyCode(), null), principal, null);
    }

    /**
     * Handles a STOMP join message and registers its WebSocket session.
     *
     * @param msg       the join message
     * @param principal the authenticated participant
     * @param headers   the STOMP headers, when available
     */
    @MessageMapping("/join")
    public void join(LobbyJoinMessage msg, @AuthenticationPrincipal Principal principal, SimpMessageHeaderAccessor headers) {
        Objects.requireNonNull(msg, "msg");
        Objects.requireNonNull(principal, "principal");
        logger.info("Join requested by {} for lobby {}", principal.getName(), msg.lobbyCode());
        String username = principal.getName();

        lobbyService.getLobby(msg.lobbyCode()).ifPresent(lobby -> {
            if (!lobby.acceptsPassword(msg.password(), passwordEncoder)) {
                logger.warn("Rejected lobby join for {} by {}", msg.lobbyCode(), username);
                return;
            }
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
     * <p>Explicit leaves also remove the participant from the WebSocket session registry
     * and notify disconnect-aware game sessions.</p>
     *
     * @param msg       the leave message
     * @param principal the authenticated participant
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
            if (lobby.listUsernames().isEmpty() && !lobby.hasActiveGame()) {
                logger.info("Lobby with code {} is empty. Deleting...", msg.lobbyCode());
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
