package de.quantumquest.triviathrone.api.game;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public interface IGameProvider {
    String gameType();

    default String gameDataFile() {
        return "game.xml";
    }

    default String gameDataFormatVersion() {
        return "1.0";
    }

    boolean supportsFile(String filename);

    Optional<IGameDefinition> load(Path gameDirectory) throws IOException;

    default Optional<IGameCreator> creator() {
        return Optional.empty();
    }

    /** Returns the plugin's browser UI entry points, if it supplies custom pages. */
    default Optional<GameUiDescriptor> uiDescriptor() {
        return Optional.empty();
    }
}
