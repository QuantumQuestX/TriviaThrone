package quantumquest.triviathrone.api.system.events;

import quantumquest.triviathrone.utils.StringUtils;

import java.time.Instant;
import java.util.Objects;

/**
 * Event emitted when a player joins a lobby.
 *
 * @param occurredAt timestamp at which the player joined
 * @param lobbyCode  code of the affected lobby
 * @param username   username of the joining player
 * @author TiJ
 * @since 2.0.0
 */
public record PlayerJoinedLobbyEvent(Instant occurredAt, String lobbyCode, String username) implements ISystemEvent {
    /**
     * Validates the immutable event payload.
     */
    public PlayerJoinedLobbyEvent {
        Objects.requireNonNull(occurredAt, "occurredAt");
        lobbyCode = StringUtils.requireText(lobbyCode, "lobbyCode");
        username = StringUtils.requireText(username, "username");
    }
}
