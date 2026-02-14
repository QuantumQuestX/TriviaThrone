package de.quantumquest.triviathrone.ws;

import de.quantumquest.triviathrone.auth.UserAccount;
import de.quantumquest.triviathrone.auth.UserConfigLoader;
import de.quantumquest.triviathrone.dto.LobbyMessage;
import de.quantumquest.triviathrone.dto.LobbyInfoMessage;
import de.quantumquest.triviathrone.server.LobbyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class WebSocketLobbyController {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketLobbyController.class);
    private final LobbyService lobbyService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketLobbyController(LobbyService lobbyService, SimpMessagingTemplate simpMessagingTemplate) {
        this.lobbyService = lobbyService;
        this.messagingTemplate = simpMessagingTemplate;
    }

    @MessageMapping("/join")
    public void join(LobbyMessage msg, @AuthenticationPrincipal Principal principal) {
        logger.info("OnJoin: {} joined lobby with code {}", principal.getName(), msg.lobbyCode());
        lobbyService.getLobby(msg.lobbyCode()).ifPresent(lobby -> {
            lobby.addUser(principal.getName());

            LobbyInfoMessage update = new LobbyInfoMessage(
                    lobby.getCode(),
                    lobby.listUsernames(),
                    lobby.getHostUser()
            );

            messagingTemplate.convertAndSend("/topic/lobby." + lobby.getCode(), update);
        });
    }

    @MessageMapping("/leave")
    public void leave(LobbyMessage msg, @AuthenticationPrincipal Principal principal) {
        logger.info("OnLeave: {} left lobby with code {}", principal.getName(), msg.lobbyCode());
        String username = principal.getName();

        lobbyService.getLobby(msg.lobbyCode()).ifPresent(lobby -> {
            lobby.removeUser(username);

            if (lobby.listUsernames().isEmpty()) {
                lobbyService.removeLobby(lobby.getCode());
                logger.info("OnLeave: lobby with code {} is empty. Deleting...", msg.lobbyCode());
            } else {
                LobbyInfoMessage update = new LobbyInfoMessage(
                        lobby.getCode(),
                        lobby.listUsernames(),
                        lobby.getHostUser()
                );

                messagingTemplate.convertAndSend(
                        "/topic/lobby." + lobby.getCode(),
                        update
                );
            }
        });
    }
}
