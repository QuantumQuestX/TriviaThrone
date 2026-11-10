package de.quantumquest.triviathrone.games.jeopardy;

import de.quantumquest.triviathrone.api.game.IGameDefinition;
import de.quantumquest.triviathrone.api.game.IGameProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

public final class JeopardyGameProvider implements IGameProvider {
    private static final String FILE_SUFFIX = ".game.throne";

    @Override
    public String gameType() {
        return "jeopardy";
    }

    @Override
    public boolean supportsFile(String filename) {
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(FILE_SUFFIX);
    }

    @Override
    public Optional<IGameDefinition> load(Path gameDirectory) throws IOException {
        if (!Files.isRegularFile(gameDirectory.resolve("manifest.xml"))) {
            return Optional.empty();
        }
        return Optional.of(GameDefinitionLoader.parse(gameDirectory));
    }
}
