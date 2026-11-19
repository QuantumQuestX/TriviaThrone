package quantumquest.triviathrone.server;

/**
 * Request body used to verify access to a lobby.
 *
 * @param password optional lobby password
 */
public record LobbyJoinRequest(String password) {
}
