package quantumquest.triviathrone.api.system.events;

import quantumquest.triviathrone.utils.StringUtils;

import java.time.Instant;
import java.util.Objects;

/**
 * Event emitted when a player leaves a lobby.
 *
 * @param occurredAt timestamp at which the player left
 * @param lobbyCode  code of the affected lobby
 * @param username   username of the departing player
 * @author TiJ
 * @since 2.0.0
 */
public record PlayerLeftLobbyEvent(Instant occurredAt, String lobbyCode, String username) implements ISystemEvent {
    /**
     * Validates the immutable event payload.
     */
    public PlayerLeftLobbyEvent {
        Objects.requireNonNull(occurredAt, "occurredAt");
        lobbyCode = StringUtils.requireText(lobbyCode, "lobbyCode");
        username = StringUtils.requireText(username, "username");
    }
}
