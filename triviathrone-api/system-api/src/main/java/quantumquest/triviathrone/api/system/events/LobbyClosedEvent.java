package quantumquest.triviathrone.api.system.events;

import quantumquest.triviathrone.utils.StringUtils;

import java.time.Instant;
import java.util.Objects;

public record LobbyClosedEvent(Instant occurredAt, String lobbyCode) implements ISystemEvent {
    public LobbyClosedEvent {
        Objects.requireNonNull(occurredAt, "occurredAt");
        lobbyCode = StringUtils.requireText(lobbyCode, "lobbyCode");
    }
}