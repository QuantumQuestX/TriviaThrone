package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.dto.lobby.LobbyDeltaMessage;
import de.quantumquest.triviathrone.dto.lobby.LobbyHostUploadMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class LobbyEventService {
    private final SimpMessagingTemplate messagingTemplate;

    public LobbyEventService(SimpMessagingTemplate simpMessagingTemplate) {
        this.messagingTemplate = Objects.requireNonNull(simpMessagingTemplate, "messagingTemplate");
    }

    public void userJoined(String lobbyCode, String lobbyHost, String username) {
        messagingTemplate.convertAndSend(
                "/topic/lobby." + lobbyCode,
                new LobbyDeltaMessage(lobbyCode, lobbyHost, username, LobbyDeltaMessage.Action.JOIN)
        );
    }

    public void userLeft(String lobbyCode, String lobbyHost, String username) {
        messagingTemplate.convertAndSend(
                "/topic/lobby." + lobbyCode,
                new LobbyDeltaMessage(lobbyCode, lobbyHost, username, LobbyDeltaMessage.Action.LEAVE)
        );
    }

    public void hostUploadedGame(String lobbyCode, String hostName, String gameName) {
        messagingTemplate.convertAndSend(
                "/topic/lobby." + lobbyCode,
                new LobbyHostUploadMessage(lobbyCode, hostName, gameName)
        );
    }
}
