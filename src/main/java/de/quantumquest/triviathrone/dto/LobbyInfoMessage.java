package de.quantumquest.triviathrone.dto;

import java.util.Set;

public record LobbyInfoMessage(String lobbyCode, Set<String> usernames, String hostUsername) { }
