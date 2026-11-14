package quantumquest.triviathrone.api.game;

import java.util.Set;

/**
 * Defines a loadable game type and creates its per-lobby sessions.
 *
 * @author TiJ
 * @since 2.0.0
 */
public interface IGameDefinition {
    /**
     * Returns the game-type identifier.
     *
     * @return a non-null identifier compatible with the supplying provider
     */
    String gameType();

    /**
     * Returns the display title of this game.
     *
     * @return a non-null title
     */
    String title();

    /**
     * Tests whether this definition references a media resource.
     *
     * @param mediaPath non-null media path to test
     * @return {@code true} when the resource is referenced
     */
    boolean referencesMedia(String mediaPath);

    /**
     * Creates a session for a lobby.
     *
     * <p>The implementation must not retain or mutate {@code players}; the returned session is owned
     * by the caller and must be closed when the lobby ends.</p>
     *
     * @param lobbyCode non-null lobby identifier
     * @param players   non-null snapshot of participating player names
     * @return a new non-null session
     */
    IGameSession createSession(String lobbyCode, Set<String> players);
}
