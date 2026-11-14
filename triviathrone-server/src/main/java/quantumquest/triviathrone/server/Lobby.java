package quantumquest.triviathrone.server;

import quantumquest.triviathrone.api.game.IGameDefinition;
import quantumquest.triviathrone.api.game.IGameSession;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class Lobby {
    private final String code;
    private final String name;
    private final String hostUser;
    private final Set<String> users = new ConcurrentSkipListSet<>();

    private volatile IGameDefinition gameDefinition;
    private volatile IGameSession gameSession;

    public Lobby(String code, String name, String hostUser) {
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.hostUser = Objects.requireNonNull(hostUser, "hostUser");
        users.add(hostUser);
    }

    public boolean addUser(String username) {
        return users.add(Objects.requireNonNull(username, "username"));
    }

    public boolean removeUser(String username) {
        return users.remove(Objects.requireNonNull(username, "username"));
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

    public synchronized void loadGame(IGameDefinition definition) {
        Objects.requireNonNull(definition, "definition");

        IGameSession previousSession = gameSession;
        gameSession = null;

        if (previousSession != null) {
            previousSession.close();
        }

        this.gameDefinition = definition;
    }

    public synchronized void startGame() {
        if (gameDefinition == null) {
            throw new IllegalStateException("No game loaded.");
        }

        if (gameSession != null) {
            throw new IllegalStateException("Game already started");
        }

        this.gameSession = gameDefinition.createSession(code, listUsernames());
    }

    public synchronized boolean startGameIfInactive() {
        if (gameSession != null) {
            return false;
        }
        startGame();
        return true;
    }

    public synchronized void endGame() {
        endGameIfActive();
    }

    public synchronized boolean endGameIfActive() {
        return endGameIfActive(gameSession);
    }

    public synchronized boolean endGameIfActive(IGameSession expectedSession) {
        if (expectedSession == null || gameSession != expectedSession) {
            return false;
        }
        gameSession = null;
        expectedSession.close();
        return true;
    }

    public boolean hasActiveGame() {
        return gameSession != null;
    }

    public boolean hasLoadedGame() {
        return gameDefinition != null;
    }

    public IGameDefinition getGameDefinition() {
        return gameDefinition;
    }

    public IGameSession getGameSession() {
        return gameSession;
    }
}
