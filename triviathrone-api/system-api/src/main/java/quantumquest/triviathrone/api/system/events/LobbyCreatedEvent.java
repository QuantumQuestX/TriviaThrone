package quantumquest.triviathrone.api.system.events;

import quantumquest.triviathrone.utils.StringUtils;

import java.time.Instant;
import java.util.Objects;

public record LobbyCreatedEvent(Instant occurredAt, String lobbyCode, String hostUsername) implements ISystemEvent {
    public LobbyCreatedEvent {
        Objects.requireNonNull(occurredAt, "occurredAt");
        lobbyCode = StringUtils.requireText(lobbyCode, "lobbyCode");
        hostUsername = StringUtils.requireText(hostUsername, "hostUsername");
    }
}