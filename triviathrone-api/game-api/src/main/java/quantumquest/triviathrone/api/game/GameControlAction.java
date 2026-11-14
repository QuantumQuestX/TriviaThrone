package quantumquest.triviathrone.api.game;

import java.util.Map;
import java.util.Optional;

/**
 * Defines a control action sent by the game protocol.
 *
 * @author TiJ
 * @since 2.0.0
 */
public enum GameControlAction {
    /**
     * Starts the game.
     */
    START_GAME("START_GAME"),
    /**
     * Ends the game.
     */
    END_GAME("END_GAME"),
    /**
     * Requests state synchronization.
     */
    SYNC("SYNC");

    /**
     * Maps protocol wire names to their corresponding actions.
     */
    private static final Map<String, GameControlAction> BY_WIRE_NAME = Map.of(
            START_GAME.wireName, START_GAME,
            END_GAME.wireName, END_GAME,
            SYNC.wireName, SYNC
    );

    /**
     * Protocol representation of this action.
     */
    private final String wireName;

    /**
     * Creates an action with its protocol representation.
     *
     * @param wireName non-null protocol representation
     */
    GameControlAction(String wireName) {
        this.wireName = wireName;
    }

    /**
     * Resolves an action from a protocol representation.
     *
     * @param wireName the wire name, or {@code null}
     * @return the matching action, or an empty optional when the name is null or unknown
     */
    public static Optional<GameControlAction> fromWireName(String wireName) {
        if (wireName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_WIRE_NAME.get(wireName));
    }

    /**
     * Returns this action's protocol representation.
     *
     * @return the non-null wire name
     */
    public String wireName() {
        return wireName;
    }
}
