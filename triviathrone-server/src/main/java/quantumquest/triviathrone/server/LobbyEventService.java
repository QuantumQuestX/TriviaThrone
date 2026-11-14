package quantumquest.triviathrone.server;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import quantumquest.triviathrone.dto.lobby.LobbyDeltaMessage;
import quantumquest.triviathrone.dto.lobby.LobbyHostUploadMessage;

import java.util.Objects;

/**
 * Publishes lobby membership and upload updates over STOMP.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Service
public class LobbyEventService {
    /**
     * Holds the messagin emplate used by this component.
     */
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Creates a LobbyEventService.
     *
     * @param simpMessagingTemplate the simp messaging template
     */
    public LobbyEventService(SimpMessagingTemplate simpMessagingTemplate) {
        this.messagingTemplate = Objects.requireNonNull(simpMessagingTemplate, "messagingTemplate");
    }

    /**
     * Broadcasts a lobby join event to that lobby's topic.
     *
     * @param lobbyCode the lobby code
     * @param lobbyHost the lobby host
     * @param username  the account username
     */
    public void userJoined(String lobbyCode, String lobbyHost, String username) {
        messagingTemplate.convertAndSend(
            "/topic/lobby." + lobbyCode,
            new LobbyDeltaMessage(lobbyCode, lobbyHost, username, LobbyDeltaMessage.Action.JOIN)
        );
    }

    /**
     * Broadcasts a lobby leave event to that lobby's topic.
     *
     * @param lobbyCode the lobby code
     * @param lobbyHost the lobby host
     * @param username  the account username
     */
    public void userLeft(String lobbyCode, String lobbyHost, String username) {
        messagingTemplate.convertAndSend(
            "/topic/lobby." + lobbyCode,
            new LobbyDeltaMessage(lobbyCode, lobbyHost, username, LobbyDeltaMessage.Action.LEAVE)
        );
    }

    /**
     * Broadcasts that a lobby host uploaded a game.
     *
     * @param lobbyCode the lobby code
     * @param hostName  the host name
     * @param gameName  the game name
     */
    public void hostUploadedGame(String lobbyCode, String hostName, String gameName) {
        messagingTemplate.convertAndSend(
            "/topic/lobby." + lobbyCode,
            new LobbyHostUploadMessage(lobbyCode, hostName, gameName)
        );
    }
}
