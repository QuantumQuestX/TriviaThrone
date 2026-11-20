# Game package format

A `.game.throne` archive must contain a `manifest.xml` at its root. The manifest identifies the game and points to its data file; game-specific data does not belong in the manifest.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<gamePackage manifestVersion="2.0">
  <mod id="example-game" version="1.0.0" apiVersion="2.0.0" apiMaximumExclusive="3.0.0"/>
  <game
      type="example-game"
      providerClass="com.example.game.ExampleGameProvider"
      dataFile="game.xml"
      formatVersion="1.0"/>
  <createdBy serverVersion="2.0.0"/>
  <metadata title="Example game" author="Author" language="en"/>
  <assets>
    <asset path="resources/cover.png" sha256="0000000000000000000000000000000000000000000000000000000000000000"/>
  </assets>
</gamePackage>
```

`manifestVersion` is the package format version. It is separate from the game data's `formatVersion`. New packages use version `2.0` and a `<mod>` element. The server also reads version `1.0` manifests that use the older `<plugin>` element.

The mod ID, game type, and provider class must match a registered provider. The server does not load a class named by uploaded XML. `apiVersion` is the inclusive minimum game API version supported by the package. `apiMaximumExclusive`, when present, is the exclusive upper bound; otherwise the package follows the same-major-version compatibility rule. Mod and server versions are informational; the server version is not used to decide compatibility.

`dataFile` and asset paths are relative to the package root. The game data file must exist. Assets are optional. If an asset entry includes a SHA-256 hash, it must match the bundled file.

After a provider creates the game data, the server writes the manifest. Each additional file in the package is listed as an asset and given a SHA-256 hash. A game editor sends the game structure and media files to the server; its provider writes `game.xml` and the resource files.

To include uploaded files, send `POST /api/game/create/{gameType}` as `multipart/form-data`. Put the JSON object in the `data` part and name each file part `asset_<resourceId>`. The server makes the files available to the creator and removes temporary uploads after generation.

Game mod JARs use `triviathrone-game-api` and load from `mods/games` at server startup. System implementations use `triviathrone-system-api` and load from `mods/system`.
