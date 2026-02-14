package de.quantumquest.triviathrone.dto.lobby;

public record LobbyDeltaMessage(String lobbyCode, String lobbyHost, String username, Action action) {
    public enum Action {
        JOIN, LEAVE
    }
}
