package quantumquest.triviathrone.api.system.events;

import java.time.Instant;

/**
 * Immutable, framework-independent server lifecycle event delivered to system providers.
 *
 * @author TiJ
 * @since 2.0.0
 */
public sealed interface ISystemEvent permits
        LobbyCreatedEvent,
        LobbyClosedEvent,
        PlayerJoinedLobbyEvent,
        PlayerLeftLobbyEvent,
        GameStartedEvent,
        GameEndedEvent {
    /**
     * Returns the instant at which this event occurred.
     */
    Instant occurredAt();
}
