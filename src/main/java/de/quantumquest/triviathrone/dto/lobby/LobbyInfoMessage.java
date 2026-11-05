package de.quantumquest.triviathrone.dto.lobby;

import java.util.Set;

public record LobbyInfoMessage(String lobbyCode, String lobbyHost, Set<String> usernames,
                               boolean gameLoaded, boolean gameStarted) {
    public LobbyInfoMessage {
        usernames = Set.copyOf(usernames);
    }
}
