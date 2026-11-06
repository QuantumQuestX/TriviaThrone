package de.quantumquest.triviathrone.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static de.quantumquest.triviathrone.server.GameUploadControllerTestSupport.LOBBY_CODE;
import static de.quantumquest.triviathrone.server.GameUploadControllerTestSupport.createFixture;
import static de.quantumquest.triviathrone.server.GameUploadControllerTestSupport.definition;
import static de.quantumquest.triviathrone.server.GameUploadControllerTestSupport.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameUploadControllerMediaTest {
    @TempDir
    Path tempDir;

    @Test
    void lobbyMemberCanLoadMediaReferencedByGame() throws IOException {
        GameUploadControllerTestSupport.Fixture fixture = createFixture(tempDir);
        fixture.lobby().loadGame(definition("resources/clue.txt"));
        Path mediaFile = fixture.storageDir().resolve(LOBBY_CODE).resolve("resources/clue.txt");
        Files.createDirectories(mediaFile.getParent());
        Files.writeString(mediaFile, "test media");

        ResponseEntity<Resource> response = fixture.controller().getGameMedia(
                LOBBY_CODE,
                "resources/clue.txt",
                user("player")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Resource body = response.getBody();
        assertNotNull(body);
        try (InputStream content = body.getInputStream()) {
            assertEquals("test media", new String(content.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void nonMemberCannotLoadLobbyMedia() {
        GameUploadControllerTestSupport.Fixture fixture = createFixture(tempDir);
        fixture.lobby().loadGame(definition("resources/clue.txt"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fixture.controller().getGameMedia(LOBBY_CODE, "resources/clue.txt", user("outsider"))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void rejectsPathsOutsideLobbyDirectory() {
        GameUploadControllerTestSupport.Fixture fixture = createFixture(tempDir);
        fixture.lobby().loadGame(definition("../outside.txt"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fixture.controller().getGameMedia(LOBBY_CODE, "../outside.txt", user("player"))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void rejectsFilesNotReferencedByGame() {
        GameUploadControllerTestSupport.Fixture fixture = createFixture(tempDir);
        fixture.lobby().loadGame(definition("resources/clue.txt"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fixture.controller().getGameMedia(LOBBY_CODE, "resources/other.txt", user("player"))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
