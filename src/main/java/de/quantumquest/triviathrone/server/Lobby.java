package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.game.GameDefinition;
import de.quantumquest.triviathrone.game.GameSession;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class Lobby {
    private final String code;
    private final String name;
    private final String hostUser;
    private final Set<String> users = new ConcurrentSkipListSet<>();

    private volatile GameDefinition gameDefinition;
    private volatile GameSession gameSession;

    public Lobby(String code, String name, String hostUser) {
        this.code = code;
        this.name = name;
        this.hostUser = hostUser;
        addUser(hostUser);
    }

    public void addUser(String username) {
        users.add(username);
    }

    public void removeUser(String username) {
        users.remove(username);
    }

    public String getLobbyCode() {
        return code;
    }

    public String getLobbyName() {
        return name;
    }

    public String getHostUsername() {
        return hostUser;
    }

    public Set<String> listUsernames() {
        return Set.copyOf(users);
    }

    public synchronized void loadGame(GameDefinition definition) {
        if (definition == null)
            throw new IllegalArgumentException("GameDefinition cannot be null");

        if (gameSession != null) gameSession.close();
        this.gameDefinition = definition;
        this.gameSession = null;
    }

    public synchronized void startGame() {
        if (gameDefinition == null) {
            throw new IllegalStateException("No game loaded.");
        }

        if (gameSession != null) {
            throw new IllegalStateException("Game already started");
        }

        this.gameSession = new GameSession(this, gameDefinition);
    }

    public synchronized void endGame() {
        if (gameSession != null) gameSession.close();
        this.gameSession = null;
    }

    public boolean hasActiveGame() {
        return gameSession != null;
    }

    public boolean hasLoadedGame() {
        return gameDefinition != null;
    }

    public GameDefinition getGameDefinition() {
        return gameDefinition;
    }

    public GameSession getGameSession() {
        return gameSession;
    }
}
