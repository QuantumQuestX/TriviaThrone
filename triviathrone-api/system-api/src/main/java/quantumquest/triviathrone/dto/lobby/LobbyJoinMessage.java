package quantumquest.triviathrone.dto.lobby;

import java.util.Objects;

/**
 * Immutable STOMP payload for joining a lobby.
 *
 * @param name participant or lobby name
 * @param lobbyCode target lobby code
 * @param password optional lobby password
 * @since 2.3.0
 */
public record LobbyJoinMessage(
    String name,
    String lobbyCode,
    String password
) {
    /**
     * Validates the fields required for every join request.
     */
    public LobbyJoinMessage {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lobbyCode, "lobbyCode");
    }
}
