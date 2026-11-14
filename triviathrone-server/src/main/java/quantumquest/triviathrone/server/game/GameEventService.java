package quantumquest.triviathrone.server.game;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import quantumquest.triviathrone.api.game.IGameSession;

import java.util.Objects;

/**
 * Publishes game state and completion messages over STOMP.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Service
public class GameEventService {
    /**
     * Holds the messagin emplate used by this component.
     */
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Creates a GameEventService.
     *
     * @param messagingTemplate the messaging template
     */
    public GameEventService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = Objects.requireNonNull(messagingTemplate, "messagingTemplate");
    }

    /**
     * Broadcasts the current game state to a lobby's game topic.
     *
     * @param lobbyCode the lobby code
     * @param session   the session
     */
    public void broadcastState(String lobbyCode, IGameSession session) {
        if (session == null) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/game." + lobbyCode, session.stateMessage());
    }

    /**
     * Broadcasts a game-completed message to a lobby's game topic.
     *
     * @param lobbyCode    the lobby code
     * @param session      the session
     * @param hostUsername the host username
     */
    public void broadcastGameEnded(String lobbyCode, IGameSession session, String hostUsername) {
        messagingTemplate.convertAndSend(
            "/topic/game." + lobbyCode,
            session.endedMessage(hostUsername)
        );
    }
}
