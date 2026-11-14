package quantumquest.triviathrone.api.game;

/**
 * Reports whether a game action was accepted and whether it ended the game.
 *
 * @param accepted  whether the action was accepted
 * @param gameEnded whether the game has ended as a result of the action
 * @author TiJ
 * @since 2.0.0
 */
public record GameActionResult(boolean accepted, boolean gameEnded) {
    /**
     * Creates a result that rejects the action without ending the game.
     *
     * @return an immutable rejection result
     */
    public static GameActionResult rejected() {
        return new GameActionResult(false, false);
    }

    /**
     * Creates a result that accepts the action without ending the game.
     *
     * @return an immutable accepted result
     */
    public static GameActionResult allow() {
        return new GameActionResult(true, false);
    }

    /**
     * Creates a result that accepts the action and ends the game.
     *
     * @return an immutable game-ending result
     */
    public static GameActionResult endGame() {
        return new GameActionResult(true, true);
    }
}
