package quantumquest.triviathrone.api.system.events;

import quantumquest.triviathrone.utils.StringUtils;

import java.time.Instant;
import java.util.Objects;

/**
 * Event emitted when a lobby closes.
 *
 * @param occurredAt timestamp at which the lobby closed
 * @param lobbyCode  code of the closed lobby
 * @author TiJ
 * @since 2.0.0
 */
public record LobbyClosedEvent(Instant occurredAt, String lobbyCode) implements ISystemEvent {
    /**
     * Validates the immutable event payload.
     */
    public LobbyClosedEvent {
        Objects.requireNonNull(occurredAt, "occurredAt");
        lobbyCode = StringUtils.requireText(lobbyCode, "lobbyCode");
    }
}
