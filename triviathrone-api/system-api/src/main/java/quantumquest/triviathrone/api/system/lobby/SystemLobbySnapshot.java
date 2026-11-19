package quantumquest.triviathrone.api.system.lobby;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable, server-independent view of an active lobby.
 *
 * @param lobbyCode lobby code
 * @param lobbyName lobby name
 * @param hostUsername host username
 * @param playerCount number of current members
 * @param gameType loaded game type, if any
 * @param gameActive whether a game session is currently active
 * @author TiJ
 * @since 2.3.0
 */
public record SystemLobbySnapshot(
    String lobbyCode,
    String lobbyName,
    String hostUsername,
    int playerCount,
    Optional<String> gameType,
    boolean gameActive
) {
    /**
     * Validates the immutable snapshot.
     */
    public SystemLobbySnapshot {
        Objects.requireNonNull(lobbyCode, "lobbyCode");
        Objects.requireNonNull(lobbyName, "lobbyName");
        Objects.requireNonNull(hostUsername, "hostUsername");
        Objects.requireNonNull(gameType, "gameType");

        if (playerCount < 0) {
            throw new IllegalArgumentException("playerCount must not be negative.");
        }
    }
}
