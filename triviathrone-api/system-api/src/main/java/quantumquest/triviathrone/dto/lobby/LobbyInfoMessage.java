package quantumquest.triviathrone.dto.lobby;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable snapshot of lobby state for a client.
 *
 * @param lobbyCode   lobby code
 * @param lobbyHost   username of the lobby host
 * @param usernames   immutable snapshot of lobby member usernames
 * @param gameLoaded  whether a game file is loaded
 * @param gameStarted whether the loaded game has started
 * @author TiJ
 * @since 2.0.0
 */
public record LobbyInfoMessage(String lobbyCode, String lobbyHost, Set<String> usernames,
                               boolean gameLoaded, boolean gameStarted) {
    /**
     * Validates scalar values and defensively copies member names.
     */
    public LobbyInfoMessage {
        Objects.requireNonNull(lobbyCode, "lobbyCode");
        Objects.requireNonNull(lobbyHost, "lobbyHost");
        usernames = Set.copyOf(Objects.requireNonNull(usernames, "usernames"));
    }
}
