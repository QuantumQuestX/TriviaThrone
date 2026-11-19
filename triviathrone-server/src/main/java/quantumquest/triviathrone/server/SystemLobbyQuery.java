package quantumquest.triviathrone.server;

import quantumquest.triviathrone.api.system.lobby.ISystemLobbyQuery;
import quantumquest.triviathrone.api.system.lobby.SystemLobbySnapshot;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Adapts active server lobbies to the system-mod API.
 *
 * @author TiJ
 * @since 2.3.0
 */
public class SystemLobbyQuery implements ISystemLobbyQuery {
    private final Supplier<LobbyService> lobbyService;

    /**
     * Creates a lazily resolved lobby query to avoid a system-mod startup cycle.
     *
     * @param lobbyService provider for the lobby service
     */
    public SystemLobbyQuery(Supplier<LobbyService> lobbyService) {
        this.lobbyService = Objects.requireNonNull(lobbyService, "lobbyService");
    }

    @Override
    public List<SystemLobbySnapshot> listLobbies() {
        return lobbyService.get().listLobbies().stream()
            .map(SystemLobbyQuery::snapshot)
            .toList();
    }

    @Override
    public Optional<SystemLobbySnapshot> findLobby(String lobbyCode) {
        Objects.requireNonNull(lobbyCode, "lobbyCode");
        return lobbyService.get().getLobby(Objects.requireNonNull(lobbyCode, "lobbyCode"))
            .map(SystemLobbyQuery::snapshot);
    }

    private static SystemLobbySnapshot snapshot(Lobby lobby) {
        var definition = lobby.getGameDefinition();
        return new SystemLobbySnapshot(
            lobby.getLobbyCode(),
            lobby.getLobbyName(),
            lobby.getHostUsername(),
            lobby.listUsernames().size(),
            definition == null ? Optional.empty() : Optional.of(definition.gameType()),
            lobby.hasActiveGame()
        );
    }
}
