package quantumquest.triviathrone.api.system.lobby;

import java.util.List;
import java.util.Optional;

/**
 * Read-only access to the server's currently active lobbies.
 *
 * @author TiJ
 * @since 2.3.0
 */
public interface ISystemLobbyQuery {
    /**
     * Returns immutable snapshots of all currently active lobbies.
     */
    List<SystemLobbySnapshot> listLobbies();

    /**
     * Finds an active lobby by its code.
     *
     * @param lobbyCode lobby code
     * @return the matching snapshot, if the lobby is active
     */
    Optional<SystemLobbySnapshot> findLobby(String lobbyCode);
}
