package quantumquest.triviathrone.api.system.events;

import quantumquest.triviathrone.utils.StringUtils;

import java.time.Instant;
import java.util.Objects;

/**
 * Event emitted when a participant's WebSocket connection closes.
 *
 * @param occurredAt timestamp at which the connection closed
 * @param lobbyCode  code of the affected lobby
 * @param username   username of the disconnected participant
 * @author TiJ
 * @since 2.2.0
 */
public record PlayerDisconnectedEvent(Instant occurredAt, String lobbyCode, String username) implements ISystemEvent {
    /**
     * Validates the immutable event payload.
     */
    public PlayerDisconnectedEvent {
        Objects.requireNonNull(occurredAt, "occurredAt");
        lobbyCode = StringUtils.requireText(lobbyCode, "lobbyCode");
        username = StringUtils.requireText(username, "username");
    }
}
