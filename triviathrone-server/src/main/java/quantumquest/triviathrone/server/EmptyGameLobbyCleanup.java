package quantumquest.triviathrone.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Closes active game lobbies that remain empty beyond the reconnect grace period.
 *
 * @author TiJ
 * @since 2.3.3
 */
@Component
public final class EmptyGameLobbyCleanup {
    /**
     * Provides access to active lobbies.
     */
    private final LobbyService lobbyService;
    /**
     * Removes files belonging to closed lobbies.
     */
    private final LobbyGameFiles lobbyGameFiles;
    /**
     * Maximum duration an empty active-game lobby may remain open.
     */
    private final Duration timeout;

    /**
     * Creates an empty-game lobby cleanup task.
     *
     * @param lobbyService    service containing active lobbies
     * @param lobbyGameFiles  service managing lobby game files
     * @param timeout         maximum empty-lobby duration
     */
    public EmptyGameLobbyCleanup(
            LobbyService lobbyService,
            LobbyGameFiles lobbyGameFiles,
            @Value("${triviathrone.lobby.empty-active-game-timeout:30s}") Duration timeout
    ) {
        this.lobbyService = Objects.requireNonNull(lobbyService, "lobbyService");
        this.lobbyGameFiles = Objects.requireNonNull(lobbyGameFiles, "lobbyGameFiles");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Lobby cleanup timeout must be positive.");
        }
        this.timeout = timeout;
    }

    /**
     * Closes active-game lobbies that have exceeded the configured empty duration.
     */
    @Scheduled(fixedDelayString = "${triviathrone.lobby.empty-active-game-cleanup-interval:10s}")
    public void closeExpiredLobbies() {
        Instant cutoff = Instant.now().minus(timeout);
        for (Lobby lobby : lobbyService.listLobbies()) {
            Instant emptySince = lobby.getEmptySince();
            if (!lobby.hasActiveGame() || emptySince == null || emptySince.isAfter(cutoff)) {
                continue;
            }
            if (lobby.endGameIfActive()) {
                lobbyService.removeLobby(lobby.getLobbyCode());
                try {
                    lobbyGameFiles.deleteLobbyFiles(lobby.getLobbyCode());
                } catch (IOException exception) {
                    throw new IllegalStateException(
                            "Could not delete uploaded files for expired lobby " + lobby.getLobbyCode(),
                            exception
                    );
                }
            }
        }
    }
}
