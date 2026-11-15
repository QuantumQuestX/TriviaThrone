# TriviaThrone

TriviaThrone is a trivia game server with a browser client. The server discovers game and system implementations from external mods.

## Running locally

You will need Java 25, Node.js, and npm.

Start the application from the project root:

```sh
./mvnw -pl triviathrone-application -am spring-boot:run
```

Run the Java tests with `./mvnw test`. Run the client tests with `npm ci` followed by `npm test` (the npm test command also runs formatting and lint checks).

## Modules

- `triviathrone-api` contains shared game, account, repository, and migration interfaces.
- `triviathrone-server` handles lobbies, authentication, and transport.
- `triviathrone-client` contains the browser client.
- `triviathrone-mod-system` supports discovery and loading of external game and system mods.
- `triviathrone-application` assembles and starts the application.

For more on the modules and migrations, see [HELP.md](HELP.md).

## Admin account

The server loads game mods from `mods/games` and system mods from `mods/system`. Account storage and migrations are provided by a system mod. To configure the initial administrator, set both `AUTH_INITIAL_ADMIN_USERNAME` and `AUTH_INITIAL_ADMIN_PASSWORD`.

## Contributions

AI-generated contributions are not accepted, and commits made by AI will not be accepted. Contributions must be the work of a human contributor.

## License

MIT. See [LICENSE](LICENSE).
