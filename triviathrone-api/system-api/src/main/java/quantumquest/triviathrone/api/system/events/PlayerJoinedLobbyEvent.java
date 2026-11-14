package quantumquest.triviathrone.api.system.events;

import quantumquest.triviathrone.utils.StringUtils;

import java.time.Instant;
import java.util.Objects;

public record PlayerJoinedLobbyEvent(Instant occurredAt, String lobbyCode, String username) implements ISystemEvent {
    public PlayerJoinedLobbyEvent {
        Objects.requireNonNull(occurredAt, "occurredAt");
        lobbyCode = StringUtils.requireText(lobbyCode, "lobbyCode");
        username = StringUtils.requireText(username, "username");
    }
}