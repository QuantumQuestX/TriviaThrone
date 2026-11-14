package quantumquest.triviathrone.api.game;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Loads game definitions and exposes metadata for one game-package format.
 *
 * @author TiJ
 * @since 2.0.0
 */
public interface IGameProvider {
    /**
     * Returns the identifier for games supplied by this provider.
     *
     * @return a non-null game-type identifier
     */
    String gameType();

    /**
     * Returns the package-relative data-file path used by this provider.
     *
     * @return the non-null default path {@code game.xml}
     */
    default String gameDataFile() {
        return "game.xml";
    }

    /**
     * Returns the provider's game-data format version.
     *
     * @return the non-null default format version {@code 1.0}
     */
    default String gameDataFormatVersion() {
        return "1.0";
    }

    /**
     * Tests whether this provider can load a package data file.
     *
     * @param filename non-null package-relative filename
     * @return {@code true} if the file is supported
     */
    boolean supportsFile(String filename);

    /**
     * Loads a game definition from an existing game directory.
     *
     * @param gameDirectory non-null directory containing the game data
     * @return a loaded definition, or an empty optional when no compatible game is present
     * @throws IOException if the directory or game data cannot be read
     */
    Optional<IGameDefinition> load(Path gameDirectory) throws IOException;

    /**
     * Returns this provider's optional game creator.
     *
     * @return an empty optional by default
     */
    default Optional<IGameCreator> creator() {
        return Optional.empty();
    }

    /**
     * Returns the mod's browser UI entry points, if it supplies custom pages.
     *
     * @return an empty optional by default
     */
    default Optional<GameUiDescriptor> uiDescriptor() {
        return Optional.empty();
    }
}
