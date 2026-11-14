package quantumquest.triviathrone.api.system.events;

import quantumquest.triviathrone.utils.StringUtils;

import java.time.Instant;
import java.util.Objects;

public record PlayerLeftLobbyEvent(Instant occurredAt, String lobbyCode, String username) implements ISystemEvent {
    public PlayerLeftLobbyEvent {
        Objects.requireNonNull(occurredAt, "occurredAt");
        lobbyCode = StringUtils.requireText(lobbyCode, "lobbyCode");
        username = StringUtils.requireText(username, "username");
    }
}
