package quantumquest.triviathrone.dto.lobby;

import java.util.Objects;

public record LobbyJoinMessage(
    String name,
    String lobbyCode,
    String password
) {
    public LobbyJoinMessage {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lobbyCode, "lobbyCode");
    }
}
