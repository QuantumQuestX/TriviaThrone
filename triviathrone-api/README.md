# TriviaThrone APIs

The API module builds two artifacts. Use the game API when writing a game mod; use the system API for server-side integrations such as storage providers.

## Game mods

Game mods are loaded from `mods/games` and should depend on `triviathrone-game-api`. Set `triviathrone.mods.games-directory` to use another directory. The older `triviathrone.plugin.directory` setting is still read as a fallback.

When building a mod, declare `io.github.quantumquestx:triviathrone-game-api` as a `provided` dependency. The release workflow publishes it to Maven Central and attaches the JAR to GitHub releases.

## System mods

System mods are trusted server code, not uploaded game content. The server discovers them from `mods/system` with Java `ServiceLoader`. Each JAR lists its `ISystemProvider` implementations in:

```text
META-INF/services/quantumquest.triviathrone.api.system.ISystemProvider
```

Depend on `io.github.quantumquestx:triviathrone-system-api` as `provided`, bundle other runtime dependencies, and register the services your provider implements. Provider registrations must be unique. Providers start in JAR-name order and close in reverse order.

The provider declares an ID, version, and API version, and receives its own data directory and configuration. Configuration properties use `triviathrone.system.<provider-id>.*`; the prefix is removed before properties reach the provider.

The server checks the API version in the provider descriptor. A provider can also declare its required major version with `@SystemCompatibility(major = 2)`; incompatible providers are rejected.

System providers may receive immutable lobby and game lifecycle events through `ISystemProvider.onEvent`. Delivery is synchronous and may happen concurrently, so handlers should be quick and thread-safe. Failures are logged without interrupting server operations or delivery to other providers. Events currently cover lobby creation and closure, player joins and leaves, and game starts and ends.

The host may expose optional typed services through `SystemServiceRegistry`. Current hosts provide
`ISystemLobbyQuery`, which returns immutable snapshots of active lobbies. Providers should use
`services.find(ISystemLobbyQuery.class)` and continue to work when the service is absent.

`ISystemLobbyQuery.listLobbies()` returns all active lobbies, while
`findLobby(String lobbyCode)` returns one matching lobby when it exists. Each
`SystemLobbySnapshot` contains the lobby code, lobby name, host username, player
count, optional loaded game type, and whether a game session is active. The
snapshot contains no mutable server objects and must be treated as a point-in-time
view.

For example, a provider can opt in during initialization:

```java
services.find(ISystemLobbyQuery.class).ifPresent(query -> {
    List<SystemLobbySnapshot> lobbies = query.listLobbies();
});
```

The current system API version is `2.3.0`. Providers must declare the same major
version and may require a version no newer than the server's version. Providers
using older compatible 2.x versions remain supported; providers requiring a
future version are rejected during startup.
