package quantumquest.triviathrone.api.game;

/**
 * Optional capability for sessions that provide private participant state.
 *
 * <p>Game sessions that do not implement this interface continue to use the public
 * state-message contract.</p>
 *
 * @author TiJ
 * @since 2.2.0
 */
public interface IPrivateGameSession extends IGameSession {
    /**
     * Creates the current state message for one participant.
     *
     * @param username non-null participant username
     * @return a message for the specified participant, or {@code null} when no message is available
     */
    IGamePrivateMessage privateStateMessage(String username);
}
