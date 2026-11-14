package quantumquest.triviathrone.api.system.events;

import java.time.Instant;

/** Immutable, framework-independent server lifecycle event delivered to system providers. */
public sealed interface ISystemEvent permits
        LobbyCreatedEvent,
        LobbyClosedEvent,
        PlayerJoinedLobbyEvent,
        PlayerLeftLobbyEvent,
        GameStartedEvent,
        GameEndedEvent {
    Instant occurredAt();
}
