package quantumquest.triviathrone.server;

import quantumquest.triviathrone.mod.GameModManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyGameFilesTest {
    @TempDir
    Path tempDir;

    @Test
    void refusesToDeleteDirectoriesOutsideGameStorage() throws IOException {
        Path storageDir = tempDir.resolve("games");
        Path outsideFile = tempDir.resolve("outside.txt");
        Files.writeString(outsideFile, "keep");

        try (GameModManager gameModManager = GameModManager.load(tempDir.resolve("mods/games"))) {
            LobbyGameFiles gameFiles = new LobbyGameFiles(
                    storageDir.toString(),
                    new GameProviderRegistry(gameModManager)
            );

            assertThrows(IllegalArgumentException.class, () -> gameFiles.deleteLobbyFiles("../"));
            assertTrue(Files.exists(outsideFile));
        }
    }
}
