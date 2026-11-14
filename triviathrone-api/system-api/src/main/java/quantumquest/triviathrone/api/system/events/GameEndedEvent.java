package quantumquest.triviathrone.api.system.events;

import quantumquest.triviathrone.utils.StringUtils;

import java.time.Instant;
import java.util.Objects;

public record GameEndedEvent(Instant occurredAt, String lobbyCode, String gameType) implements ISystemEvent {
    public GameEndedEvent {
        Objects.requireNonNull(occurredAt, "occurredAt");
        lobbyCode = StringUtils.requireText(lobbyCode, "lobbyCode");
        gameType = StringUtils.requireText(gameType, "gameType");
    }
}
