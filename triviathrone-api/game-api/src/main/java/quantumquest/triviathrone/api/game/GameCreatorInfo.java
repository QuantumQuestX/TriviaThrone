package quantumquest.triviathrone.api.game;

import java.util.Objects;

/**
 * Associates a game type with its creator descriptor.
 *
 * @param gameType   non-null game-type identifier
 * @param descriptor non-null creator descriptor
 * @author TiJ
 * @since 2.0.0
 */
public record GameCreatorInfo(
    String gameType,
    GameCreatorDescriptor descriptor
) {
    /**
     * Validates the creator information.
     *
     * @throws NullPointerException if either component is null
     */
    public GameCreatorInfo {
        Objects.requireNonNull(gameType, "gameType");
        Objects.requireNonNull(descriptor, "descriptor");
    }
}
