package quantumquest.triviathrone.ws;

import quantumquest.triviathrone.api.system.events.GameEndedEvent;
import quantumquest.triviathrone.api.system.events.ISystemEvent;
import quantumquest.triviathrone.api.system.events.PlayerJoinedLobbyEvent;
import quantumquest.triviathrone.api.system.events.PlayerLeftLobbyEvent;
import quantumquest.triviathrone.dto.lobby.LobbyCommonMessage;
import quantumquest.triviathrone.mod.SystemModManager;
import quantumquest.triviathrone.server.LobbyEventService;
import quantumquest.triviathrone.server.LobbyGameFiles;
import quantumquest.triviathrone.server.LobbyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.security.Principal;
import java.time.Instant;
import java.util.Objects;

@Controller
public class WebSocketLobbyController {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketLobbyController.class);
    private final LobbyService lobbyService;
    private final LobbyEventService lobbyEventService;
    private final LobbyGameFiles lobbyGameFiles;
    private final SystemModManager systemMods;

    public WebSocketLobbyController(
            LobbyService lobbyService,
            LobbyEventService lobbyEventService,
            LobbyGameFiles lobbyGameFiles,
            SystemModManager systemMods
    ) {
        this.lobbyService = Objects.requireNonNull(lobbyService, "lobbyService");
        this.lobbyEventService = Objects.requireNonNull(lobbyEventService, "lobbyEventService");
        this.lobbyGameFiles = Objects.requireNonNull(lobbyGameFiles, "lobbyGameFiles");
        this.systemMods = Objects.requireNonNull(systemMods, "systemMods");
    }

    @MessageMapping("/join")
    public void join(LobbyCommonMessage msg, @AuthenticationPrincipal Principal principal) {
        Objects.requireNonNull(msg, "msg");
        Objects.requireNonNull(principal, "principal");
        logger.info("OnJoin: {} joined lobby with code {}", principal.getName(), msg.lobbyCode());
        String username = principal.getName();

        lobbyService.getLobby(msg.lobbyCode()).ifPresent(lobby -> {
            boolean joined = lobby.addUser(username);
            if (joined) {
                systemMods.publishEvent(new PlayerJoinedLobbyEvent(Instant.now(), msg.lobbyCode(), username));
            }
            lobbyEventService.userJoined(msg.lobbyCode(), lobby.getHostUsername(), username);
        });
    }

    @MessageMapping("/leave")
    public void leave(LobbyCommonMessage msg, @AuthenticationPrincipal Principal principal) {
        Objects.requireNonNull(msg, "msg");
        Objects.requireNonNull(principal, "principal");
        logger.info("OnLeave: {} left lobby with code {}", principal.getName(), msg.lobbyCode());
        String username = principal.getName();

        lobbyService.getLobby(msg.lobbyCode()).ifPresent(lobby -> {
            boolean left = lobby.removeUser(username);
            if (left) {
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
