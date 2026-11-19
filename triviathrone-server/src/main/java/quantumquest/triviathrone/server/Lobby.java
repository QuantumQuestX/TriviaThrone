package quantumquest.triviathrone.server;

import org.springframework.security.crypto.password.PasswordEncoder;
import quantumquest.triviathrone.api.game.IGameDefinition;
import quantumquest.triviathrone.api.game.IGameMessage;
import quantumquest.triviathrone.api.game.IGameSession;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Represents a lobby, its members, and its optional game session.
 *
 * @author TiJ
 * @since 2.0.0
 */
public class Lobby {
    /**
     * Holds the code used by this component.
     */
    private final String code;
    /**
     * Holds the name used by this component.
     */
    private final String name;
    /**
     * Holds the hos ser used by this component.
     */
    private final String hostUser;
    private final String passwordHash;
    /**
     * Stores an internal helper required by this component.
     */
    private final Set<String> users = new ConcurrentSkipListSet<>();

    /**
     * Holds the gam efinition used by this component.
     */
    private volatile IGameDefinition gameDefinition;
    /**
     * Holds the gam ession used by this component.
     */
    private volatile IGameSession gameSession;
    /**
     * Holds the last completed game message for synchronization after the session is closed.
     */
    private volatile IGameMessage completedGameMessage;

    /**
     * Creates a Lobby.
     *
     * @param code     the lobby code
     * @param name     the lobby name
     * @param hostUser the host username
     */
    public Lobby(String code, String name, String hostUser) {
        this(code, name, hostUser, null);
    }

    public Lobby(String code, String name, String hostUser, String passwordHash) {
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.hostUser = Objects.requireNonNull(hostUser, "hostUser");
        this.passwordHash = passwordHash;
        users.add(hostUser);
    }

    /**
     * Adds a user to this lobby.
     *
     * @param username the account username
     * @return the generated result
     */
    public boolean addUser(String username) {
        return users.add(Objects.requireNonNull(username, "username"));
    }

    /**
     * Removes a user from this lobby.
     *
     * @param username the account username
     * @return the generated result
     */
    public boolean removeUser(String username) {
        return users.remove(Objects.requireNonNull(username, "username"));
    }

    /**
     * Stores the terminal message of the completed game.
     *
     * @param message non-null completed-game message
     */
    public void rememberCompletedGame(IGameMessage message) {
        this.completedGameMessage = Objects.requireNonNull(message, "message");
    }

    /**
     * Removes the cached completed-game message.
     */
    public void clearCompletedGame() {
        this.completedGameMessage = null;
    }

    /**
     * Returns the cached terminal message of the last completed game.
     *
     * @return the completed-game message, or {@code null} when none is cached
     */
    public IGameMessage getCompletedGameMessage() {
        return completedGameMessage;
    }

    public boolean isPasswordProtected() {
        return passwordHash != null;
    }

    /**
     * Returns the lobby code.
     */
    public String getLobbyCode() {
        return code;
    }

    /**
     * Returns the lobby name.
     */
    public String getLobbyName() {
        return name;
    }

    /**
     * Returns the host username.
     */
    public String getHostUsername() {
        return hostUser;
    }

    /**
     * Returns an immutable snapshot of lobby members.
     *
     * @return an immutable snapshot of lobby members
     */
    public Set<String> listUsernames() {
        return Set.copyOf(users);
    }

    public boolean acceptsPassword(String password, PasswordEncoder passwordEncoder) {
        Objects.requireNonNull(passwordEncoder, "passwordEncoder");
        return passwordHash == null || password != null && passwordEncoder.matches(password, passwordHash);
    }

    /**
     * Replaces the loaded game definition and closes any active session.
     *
     * @param definition the definition
     */
    public synchronized void loadGame(IGameDefinition definition) {
        Objects.requireNonNull(definition, "definition");

        IGameSession previousSession = gameSession;
        gameSession = null;

        if (previousSession != null) {
            previousSession.close();
        }

        this.gameDefinition = definition;
    }

    /**
     * Starts a session for the loaded game.
     */
    public synchronized void startGame() {
        if (gameDefinition == null) {
            throw new IllegalStateException("No game loaded.");
        }

        if (gameSession != null) {
            throw new IllegalStateException("Game already started");
        }

        this.gameSession = gameDefinition.createSession(code, listUsernames());
    }

    /**
     * Starts the loaded game only when no session is active.
     *
     * @return the generated result
     */
    public synchronized boolean startGameIfInactive() {
        if (gameSession != null) {
            return false;
        }
        startGame();
        return true;
    }

    /**
     * Ends the active game session when one exists.
     */
    public synchronized void endGame() {
        endGameIfActive();
    }

    /**
     * Ends the expected active game session.
     *
     * @return the generated result
     */
    public synchronized boolean endGameIfActive() {
        return endGameIfActive(gameSession);
    }

    /**
     * Ends the expected active game session.
     *
     * @param expectedSession the expected session
     * @return the generated result
     */
    public synchronized boolean endGameIfActive(IGameSession expectedSession) {
        if (expectedSession == null || gameSession != expectedSession) {
            return false;
        }
        gameSession = null;
        expectedSession.close();
        return true;
    }

    /**
     * Reports whether active game.
     *
     * @return whether a game session is active
     */
    public boolean hasActiveGame() {
        return gameSession != null;
    }

    /**
     * Reports whether loaded game.
     *
     * @return whether a game definition is loaded
     */
    public boolean hasLoadedGame() {
        return gameDefinition != null;
    }

    /**
     * Returns the game definition.
     */
    public IGameDefinition getGameDefinition() {
        return gameDefinition;
    }

    /**
     * Returns the game session.
     */
    public IGameSession getGameSession() {
        return gameSession;
    }
}
