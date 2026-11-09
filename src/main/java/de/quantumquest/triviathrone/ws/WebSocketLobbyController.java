package de.quantumquest.triviathrone.ws;

import de.quantumquest.triviathrone.dto.lobby.LobbyCommonMessage;
import de.quantumquest.triviathrone.server.LobbyEventService;
import de.quantumquest.triviathrone.server.LobbyGameFiles;
import de.quantumquest.triviathrone.server.LobbyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.security.Principal;
import java.util.Objects;

@Controller
public class WebSocketLobbyController {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketLobbyController.class);
    private final LobbyService lobbyService;
    private final LobbyEventService lobbyEventService;
    private final LobbyGameFiles lobbyGameFiles;

    public WebSocketLobbyController(
            LobbyService lobbyService,
            LobbyEventService lobbyEventService,
            LobbyGameFiles lobbyGameFiles
    ) {
        this.lobbyService = Objects.requireNonNull(lobbyService, "lobbyService");
        this.lobbyEventService = Objects.requireNonNull(lobbyEventService, "lobbyEventService");
        this.lobbyGameFiles = Objects.requireNonNull(lobbyGameFiles, "lobbyGameFiles");
    }

    @MessageMapping("/join")
    public void join(LobbyCommonMessage msg, @AuthenticationPrincipal Principal principal) {
        Objects.requireNonNull(msg, "msg");
        Objects.requireNonNull(principal, "principal");
        logger.info("OnJoin: {} joined lobby with code {}", principal.getName(), msg.lobbyCode());
        String username = principal.getName();

        lobbyService.getLobby(msg.lobbyCode()).ifPresent(lobby -> {
            lobby.addUser(username);
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
            lobby.removeUser(username);
            lobbyEventService.userLeft(msg.lobbyCode(), lobby.getHostUsername(), username);
            if (lobby.listUsernames().isEmpty()) {
                logger.info("Lobby with code {} is empty. Deleting...", msg.lobbyCode());
                lobby.endGame();
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
