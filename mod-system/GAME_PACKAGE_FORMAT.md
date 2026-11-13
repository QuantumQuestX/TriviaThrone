# Game package format

Every `.game.throne` archive has a root-level `manifest.xml`. It is the
mod-independent package entry point; game-specific data belongs in the
file named by the manifest, not in `manifest.xml`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<gamePackage manifestVersion="2.0">
  <mod id="jeopardy" version="1.0.0" apiVersion="2.0.0"/>
  <game
      type="jeopardy"
      providerClass="quantumquest.triviathrone.games.jeopardy.JeopardyGameProvider"
      dataFile="game.xml"
      formatVersion="1.0"/>
  <createdBy serverVersion="2.0.0"/>
  <metadata title="Example game" author="Author" language="en"/>
  <assets>
    <asset path="resources/cover.png" sha256="0000000000000000000000000000000000000000000000000000000000000000"/>
  </assets>
</gamePackage>
```

`manifestVersion` versions this package contract independently from the
game-data `formatVersion`. The mod ID, game type, and provider class are
matched against already registered providers; the class name is never loaded
from the uploaded XML. The package API version must match the installed
mod's declared API version. Mod and server versions are diagnostic
metadata; the server build is not used as a compatibility gate.
New packages use manifest version `2.0` and a `<mod>` element. The server
continues to read version `1.0` manifests that use the former `<plugin>`
element.

`dataFile` and asset paths are package-relative paths. The referenced game
data file must exist. Asset entries are optional; when an asset has a SHA-256
value, the bundled file must match it.

The server-side game creator writes the package manifest after the provider
creates its game data. Every additional file in the generated package is listed
as an asset and receives a SHA-256 hash. The Jeopardy editor submits its game
structure and media files to the server; the Jeopardy provider writes
`game.xml` and the bundled resource files.

Creators that need uploaded files can use `POST /api/game/create/{gameType}`
with `multipart/form-data`: the `data` part contains the JSON object and each
binary part is named `asset_<resourceId>`. The server exposes those files to the
creator as request assets and removes the temporary uploads after generation.

Game mod JARs depend on `triviathrone-game-api` and are loaded from
`mods/games` at server startup. System implementations target
`triviathrone-system-api`; the built-in H2 implementation is supplied by the
`infrastructure` module.
