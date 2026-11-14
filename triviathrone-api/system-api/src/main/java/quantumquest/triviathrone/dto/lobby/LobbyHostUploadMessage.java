package quantumquest.triviathrone.dto.lobby;

/**
 * Immutable payload describing a game file uploaded by a lobby host.
 *
 * @param lobbyCode receiving lobby code
 * @param hostName  name of the uploading host
 * @param fileName  uploaded file name
 * @author TiJ
 * @since 2.0.0
 */
public record LobbyHostUploadMessage(String lobbyCode, String hostName, String fileName) {
}
