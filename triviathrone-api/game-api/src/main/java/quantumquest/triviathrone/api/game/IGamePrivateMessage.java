package quantumquest.triviathrone.api.game;

/**
 * Marks a game message intended for one participant only.
 *
 * @author TiJ
 * @since 2.2.0
 */
public interface IGamePrivateMessage extends IGameMessage {
    /**
     * Returns the username of the only participant allowed to receive this message.
     *
     * @return the non-null recipient username
     */
    String recipient();
}
