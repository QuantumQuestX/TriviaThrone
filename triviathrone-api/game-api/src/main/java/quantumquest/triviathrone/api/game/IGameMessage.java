package quantumquest.triviathrone.api.game;

/**
 * Marks a serializable message emitted by a game definition or session.
 *
 * @author TiJ
 * @since 2.0.0
 */
public interface IGameMessage {
    /**
     * Returns the game type that defines this message's serialization contract.
     *
     * @return a non-null game-type identifier
     */
    String gameType();
}
