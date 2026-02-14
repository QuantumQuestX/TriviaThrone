package de.quantumquest.triviathrone.ws;

import de.quantumquest.triviathrone.auth.UserAccount;
import de.quantumquest.triviathrone.auth.UserConfigLoader;
import de.quantumquest.triviathrone.dto.LobbyMessage;
import de.quantumquest.triviathrone.dto.LobbyInfoMessage;
import de.quantumquest.triviathrone.server.LobbyService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class WebSocketLobbyController {
    private final LobbyService lobbyService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserConfigLoader userConfigLoader;

    public WebSocketLobbyController(LobbyService lobbyService, SimpMessagingTemplate simpMessagingTemplate, UserConfigLoader userConfigLoader) {
        this.lobbyService = lobbyService;
        this.messagingTemplate = simpMessagingTemplate;
        this.userConfigLoader = userConfigLoader;
    }

    @MessageMapping("/join")
    public void join(LobbyMessage msg, @AuthenticationPrincipal Principal principal) {
        System.out.println("onjoin " + principal.getName());
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
        String username = principal.getName();

        lobbyService.getLobby(msg.lobbyCode()).ifPresent(lobby -> {
            lobby.removeUser(username);

            if (lobby.listUsernames().isEmpty()) {
                lobbyService.removeLobby(lobby.getCode());
            } else {
                LobbyInfoMessage update = new LobbyInfoMessage(
                        lobby.getCode(),
                        lobby.listUsernames(),
                        lobby.getHostUser()
                );

                messagingTemplate.convertAndSend(
                        "/topic/payload." + lobby.getCode(),
                        update
                );
            }
        });
    }
}
