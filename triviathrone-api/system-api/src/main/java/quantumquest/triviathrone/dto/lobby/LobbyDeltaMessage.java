package quantumquest.triviathrone.dto.lobby;

import java.util.Objects;

public record LobbyDeltaMessage(String lobbyCode, String lobbyHost, String username, Action action) {
    public LobbyDeltaMessage {
        Objects.requireNonNull(lobbyCode, "lobbyCode");
        Objects.requireNonNull(lobbyHost, "lobbyHost");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(action, "action");
    }

    public enum Action {
        JOIN, LEAVE
    }
}
