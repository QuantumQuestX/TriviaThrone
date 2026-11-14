package quantumquest.triviathrone.api.system.events;

import quantumquest.triviathrone.utils.StringUtils;

import java.time.Instant;
import java.util.Objects;

/**
 * Event emitted when a game starts in a lobby.
 *
 * @param occurredAt timestamp at which the game started
 * @param lobbyCode  code of the affected lobby
 * @param gameType   identifier of the started game type
 * @author TiJ
 * @since 2.0.0
 */
public record GameStartedEvent(Instant occurredAt, String lobbyCode, String gameType) implements ISystemEvent {
    /**
     * Validates the immutable event payload.
     */
    public GameStartedEvent {
        Objects.requireNonNull(occurredAt, "occurredAt");
        lobbyCode = StringUtils.requireText(lobbyCode, "lobbyCode");
        gameType = StringUtils.requireText(gameType, "gameType");
    }
}
