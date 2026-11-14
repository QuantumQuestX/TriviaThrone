# TriviaThrone APIs

The API reactor is split into two independently consumable artifacts:

- `triviathrone-game-api` contains the contracts used by game mods. Game mod
  JARs are loaded from `mods/games` and should depend on this artifact only.
  Set `triviathrone.mods.games-directory` to override that location; the former
  `triviathrone.plugin.directory` setting remains a fallback for existing
  deployments.
- `triviathrone-system-api` contains server-wide contracts, including account
  repositories and storage migrations. Database and other system
  implementations should depend on these contracts rather than server classes
  or Spring APIs.

The server depends on both APIs. The H2 implementation is packaged by the
`infrastructure` module as a system mod with its H2 runtime dependency bundled.
Building the full project from the repository root copies that mod into
`mods/system`; the application does not load the infrastructure module on its
own classpath.

The release workflow publishes versioned game-api and system-api JARs as
standalone assets for mod developers.

System mods are discovered from `mods/system` through Java `ServiceLoader`.
Each JAR provides one or more `ISystemProvider` implementations in
`META-INF/services/quantumquest.triviathrone.api.system.ISystemProvider`.
Providers declare an ID, version, and system API version, initialize with a
provider-scoped data directory and configuration, and register services by
interface type. Service registrations must be unique; providers are started in
JAR-name order and closed in reverse order. System configuration uses the
`triviathrone.system.<provider-id>.*` namespace.

Provider classes may declare their required system API compatibility major with
`@SystemCompatibility(major = 2)`. The system loader rejects annotated
providers whose major is incompatible with the server; the API version in the
provider descriptor is also checked.

Providers can observe immutable lobby and game lifecycle events through the
default `ISystemProvider.onEvent` callback. Events are delivered synchronously
and may be delivered concurrently; providers should keep handlers fast and
thread-safe. A provider failure is logged and does not interrupt the server
operation or delivery to other providers. The current events cover lobby
creation/closure, player join/leave, and game start/end.

System providers are trusted server code, not uploaded content. A mod should
use the system API as a provided dependency, bundle any non-platform runtime
dependencies, and declare its service file. Its configuration keys are exposed
without the `triviathrone.system.<provider-id>.` prefix. For example, the H2
mod reads `jdbc-url` from `triviathrone.system.h2.jdbc-url`; its default points
at the existing `./triviathrone-users` database file. API compatibility is
major-version based. A provider registers the services it implements, such as
`IUserAccountRepository` and `IMigrationRunner`. To replace the built-in H2
account storage, remove its JAR from `mods/system` before adding another
provider for `IUserAccountRepository`.
