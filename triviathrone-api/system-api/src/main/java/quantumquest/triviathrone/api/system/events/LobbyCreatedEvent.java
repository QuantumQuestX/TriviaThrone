package quantumquest.triviathrone.api.system.events;

import quantumquest.triviathrone.utils.StringUtils;

import java.time.Instant;
import java.util.Objects;

/**
 * Event emitted when a lobby is created.
 *
 * @param occurredAt   timestamp at which the lobby was created
 * @param lobbyCode    code of the created lobby
 * @param hostUsername username of the lobby host
 * @author TiJ
 * @since 2.0.0
 */
public record LobbyCreatedEvent(Instant occurredAt, String lobbyCode, String hostUsername) implements ISystemEvent {
    /**
     * Validates the immutable event payload.
     */
    public LobbyCreatedEvent {
        Objects.requireNonNull(occurredAt, "occurredAt");
        lobbyCode = StringUtils.requireText(lobbyCode, "lobbyCode");
        hostUsername = StringUtils.requireText(hostUsername, "hostUsername");
    }
}
