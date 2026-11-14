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

The provider declares an ID, version, and API version, and receives its own data directory and configuration. Configuration properties use `triviathrone.system.<provider-id>.*`; the prefix is removed before properties reach the provider. For example, the H2 provider reads `jdbc-url` from `triviathrone.system.h2.jdbc-url`. Its default database is `./triviathrone-users`.

The server checks the API version in the provider descriptor. A provider can also declare its required major version with `@SystemCompatibility(major = 2)`; incompatible providers are rejected.

System providers may receive immutable lobby and game lifecycle events through `ISystemProvider.onEvent`. Delivery is synchronous and may happen concurrently, so handlers should be quick and thread-safe. Failures are logged without interrupting server operations or delivery to other providers. Events currently cover lobby creation and closure, player joins and leaves, and game starts and ends.

The `infrastructure` module packages the built-in H2 implementation and its runtime dependency as a system mod. A full build copies it to `mods/system`; the application does not load the infrastructure module directly. To replace H2 account storage, remove its JAR from that directory before installing another provider for `IUserAccountRepository`.
