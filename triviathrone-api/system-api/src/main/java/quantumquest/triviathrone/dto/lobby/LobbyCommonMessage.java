package quantumquest.triviathrone.dto.lobby;

/**
 * Immutable lobby message payload containing a name and lobby code.
 *
 * @param name      participant or lobby name
 * @param lobbyCode target lobby code
 * @author TiJ
 * @since 2.0.0
 */
public record LobbyCommonMessage(String name, String lobbyCode) {
}
