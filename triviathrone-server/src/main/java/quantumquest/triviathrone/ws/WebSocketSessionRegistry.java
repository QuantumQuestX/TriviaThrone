package quantumquest.triviathrone.ws;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks authenticated users associated with WebSocket sessions.
 *
 * @author TiJ
 * @since 2.2.0
 */
@Component
public class WebSocketSessionRegistry {
    private final Map<String, Connection> connections = new ConcurrentHashMap<>();

    /**
     * Associates a WebSocket session with a lobby participant.
     *
     * @param websocketSessionId non-null WebSocket session identifier
     * @param lobbyCode          non-null lobby code
     * @param username           non-null participant username
     */
    public void register(String websocketSessionId, String lobbyCode, String username) {
        connections.put(websocketSessionId, new Connection(lobbyCode, username));
    }

    /**
     * Removes and returns the participant associated with a WebSocket session.
     *
     * @param websocketSessionId non-null WebSocket session identifier
     * @return the connection, or {@code null} when it was not registered
     */
    public Connection remove(String websocketSessionId) {
        return connections.remove(websocketSessionId);
    }

    /**
     * Removes all registered sessions for a participant in a lobby.
     *
     * @param lobbyCode non-null lobby code
     * @param username  non-null participant username
     */
    public void removeForUser(String lobbyCode, String username) {
        connections.entrySet().removeIf(
            entry -> entry.getValue().lobbyCode().equals(lobbyCode)
                && entry.getValue().username().equals(username)
        );
    }

    /**
     * Immutable association between a WebSocket session and a lobby participant.
     *
     * @param lobbyCode lobby code
     * @param username  participant username
     */
    public record Connection(String lobbyCode, String username) {
    }
}
