package de.quantumquest.triviathrone.game;

import de.quantumquest.triviathrone.server.Lobby;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class GameSessionManager {
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();

    public GameSession createSession(Lobby lobby, GameDefinition definition) {
        if (lobby == null || definition == null) {
            throw new IllegalArgumentException("Lobby and definition required");
        }

        GameSession session = new GameSession(lobby, definition);
        sessions.put(lobby.getLobbyCode(), session);
        return session;
    }

    public Optional<GameSession> getSession(String lobbyCode) {
        return Optional.ofNullable(sessions.get(lobbyCode));
    }

    public void removeSession(String lobbyCode) {
        sessions.remove(lobbyCode);
    }

    public boolean hasSession(String lobbyCode) {
        return sessions.containsKey(lobbyCode);
    }
}
