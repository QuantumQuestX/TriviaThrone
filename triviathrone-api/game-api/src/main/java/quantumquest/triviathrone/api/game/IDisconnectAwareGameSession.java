package quantumquest.triviathrone.api.game;

/**
 * Optional capability for sessions that react to participant disconnects.
 *
 * @author TiJ
 * @since 2.2.0
 */
public interface IDisconnectAwareGameSession extends IGameSession {
    /**
     * Notifies the session that a participant disconnected.
     *
     * @param username non-null disconnected participant username
     */
    void playerDisconnected(String username);
}
