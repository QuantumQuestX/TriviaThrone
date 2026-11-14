package quantumquest.triviathrone.dto.game;

import java.util.Objects;
import java.util.Map;

/**
 * Represents an action received for a game lobby.
 *
 * <p>The parameter map is an immutable snapshot. Values are not deep-copied and must be compatible
 * with the message serialization contract.</p>
 *
 * @param lobbyCode non-null lobby identifier
 * @param action non-null action identifier
 * @param parameters action parameters; {@code null} is normalized to an empty map
 * @author TiJ
 * @since 2.0.0
 */
public record GameActionMessage(String lobbyCode, String action, Map<String, Object> parameters) {
    /**
     * Validates required identifiers and snapshots parameters.
     *
     * @throws NullPointerException if {@code lobbyCode} or {@code action} is null
     */
    public GameActionMessage {
        Objects.requireNonNull(lobbyCode, "lobbyCode");
        Objects.requireNonNull(action, "action");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
