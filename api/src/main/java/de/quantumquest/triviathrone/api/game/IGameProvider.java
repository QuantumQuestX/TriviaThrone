package de.quantumquest.triviathrone.api.game;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public interface IGameProvider {
    String gameType();

    boolean supportsFile(String filename);

    Optional<IGameDefinition> load(Path gameDirectory) throws IOException;
}
