package quantumquest.triviathrone.api.game;

import quantumquest.triviathrone.dto.game.GameActionMessage;

/**
 * Manages the mutable state of one lobby's game instance.
 *
 * <p>The caller owns the session and must close it once the lobby ends. Implementations define their
 * own thread-safety policy; state-change listeners may be invoked asynchronously.</p>
 *
 * @author TiJ
 * @since 2.0.0
 */
public interface IGameSession extends AutoCloseable {
    /**
     * Handles an action submitted by a player.
     *
     * @param action   non-null action message
     * @param username non-null submitting username
     * @param isHost   whether the submitting player is the lobby host
     * @return a non-null result describing acceptance and game termination
     */
    GameActionResult handleAction(GameActionMessage action, String username, boolean isHost);

    /**
     * Creates a message representing the current game state.
     *
     * @return a non-null state message
     */
    IGameMessage stateMessage();

    /**
     * Creates the final message after the game ends.
     *
     * @param hostUsername non-null username of the lobby host
     * @return a non-null terminal-state message
     */
    IGameMessage endedMessage(String hostUsername);

    /**
     * Registers notifications for state changes that happen asynchronously outside handleAction.
     *
     * @param listener non-null callback replacing any previously registered listener
     */
    void setStateChangeListener(Runnable listener);

    /**
     * Releases resources held by this session.
     */
    @Override
    void close();
}
