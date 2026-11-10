package de.quantumquest.triviathrone.dto.lobby;

import java.util.Set;
import java.util.Objects;

public record LobbyInfoMessage(String lobbyCode, String lobbyHost, Set<String> usernames,
                               boolean gameLoaded, boolean gameStarted) {
    public LobbyInfoMessage {
        Objects.requireNonNull(lobbyCode, "lobbyCode");
        Objects.requireNonNull(lobbyHost, "lobbyHost");
        usernames = Set.copyOf(Objects.requireNonNull(usernames, "usernames"));
    }
}
