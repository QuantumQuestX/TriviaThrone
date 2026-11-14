package quantumquest.triviathrone.dto.lobby;

import java.util.Objects;

/**
 * Immutable lobby-membership change payload.
 *
 * @param lobbyCode affected lobby code
 * @param lobbyHost username of the lobby host
 * @param username  username whose membership changed
 * @param action    membership operation that occurred
 * @author TiJ
 * @since 2.0.0
 */
public record LobbyDeltaMessage(String lobbyCode, String lobbyHost, String username, Action action) {
    /**
     * Validates the non-null delta payload.
     */
    public LobbyDeltaMessage {
        Objects.requireNonNull(lobbyCode, "lobbyCode");
        Objects.requireNonNull(lobbyHost, "lobbyHost");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(action, "action");
    }

    /**
     * Membership operation represented by a lobby delta.
     *
     * @author TiJ
     * @since 2.0.0
     */
    public enum Action {
        /**
         * A player joined the lobby.
         */
        JOIN,
        /**
         * A player left the lobby.
         */
        LEAVE
    }
}
