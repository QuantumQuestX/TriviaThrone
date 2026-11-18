package quantumquest.triviathrone.api.game;

import quantumquest.triviathrone.dto.game.GameActionMessage;

/**
 * Optional capability for sessions that support player lifeline actions.
 *
 * @author TiJ
 * @since 2.2.0
 */
public interface ILifelineGameSession extends IGameSession {
    /**
     * Determines whether an action is a lifeline action handled by this session.
     *
     * @param action non-null action message
     * @return whether the action should be passed to {@link #userLifeline(String, GameActionMessage)}
     */
    boolean isLifelineAction(GameActionMessage action);

    /**
     * Applies a lifeline action submitted by a participant.
     *
     * @param username non-null submitting participant username
     * @param action   non-null lifeline action message
     * @return the result of applying the action
     */
    GameActionResult userLifeline(String username, GameActionMessage action);
}
