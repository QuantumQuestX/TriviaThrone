package de.quantumquest.triviathrone.server;

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

        LobbyGameFiles gameFiles = new LobbyGameFiles(storageDir.toString());

        assertThrows(IllegalArgumentException.class, () -> gameFiles.deleteLobbyFiles("../"));
        assertTrue(Files.exists(outsideFile));
    }
}
