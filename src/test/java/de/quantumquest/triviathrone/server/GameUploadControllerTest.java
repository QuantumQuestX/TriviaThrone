package de.quantumquest.triviathrone.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static de.quantumquest.triviathrone.server.GameUploadControllerTestSupport.LOBBY_CODE;
import static de.quantumquest.triviathrone.server.GameUploadControllerTestSupport.createFixture;
import static de.quantumquest.triviathrone.server.GameUploadControllerTestSupport.gameArchive;
import static de.quantumquest.triviathrone.server.GameUploadControllerTestSupport.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

class GameUploadControllerTest {
    @TempDir
    Path tempDir;

    @Test
    void uploadsGameIntoConfiguredStorageDirectory() throws IOException {
        GameUploadControllerTestSupport.Fixture fixture = createFixture(tempDir);
        MockMultipartFile gameFile = new MockMultipartFile(
                "file",
                "quiz.game.throne",
                "application/octet-stream",
                gameArchive()
        );

        ResponseEntity<String> response = fixture.controller().uploadGame(
                gameFile,
                LOBBY_CODE,
                user("host")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(fixture.lobby().hasLoadedGame());
        assertTrue(Files.isRegularFile(
                fixture.storageDir().resolve(LOBBY_CODE).resolve("resources/clue.txt")
        ));
        assertFalse(fixture.lobby().getGameDefinition().rounds().isEmpty());
        verify(fixture.lobbyEventService()).hostUploadedGame(
                LOBBY_CODE,
                "host",
                "quiz.game.throne"
        );
    }
}
