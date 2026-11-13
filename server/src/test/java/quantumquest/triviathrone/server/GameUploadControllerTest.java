package quantumquest.triviathrone.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static quantumquest.triviathrone.server.GameUploadControllerTestSupport.LOBBY_CODE;
import static quantumquest.triviathrone.server.GameUploadControllerTestSupport.createFixture;
import static quantumquest.triviathrone.server.GameUploadControllerTestSupport.gameArchive;
import static quantumquest.triviathrone.server.GameUploadControllerTestSupport.user;
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
        assertEquals("jeopardy", fixture.lobby().getGameDefinition().gameType());
        assertEquals("Uploaded game", fixture.lobby().getGameDefinition().title());
        verify(fixture.lobbyEventService()).hostUploadedGame(
                LOBBY_CODE,
                "host",
                "quiz.game.throne"
        );
    }

    @Test
    void rejectsArchivesWithoutThePackageManifest() throws IOException {
        GameUploadControllerTestSupport.Fixture fixture = createFixture(tempDir);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream archive = new ZipOutputStream(bytes)) {
            archive.putNextEntry(new ZipEntry("game.xml"));
            archive.write("<game version=\"1.0\"/>".getBytes(StandardCharsets.UTF_8));
            archive.closeEntry();
        }
        MockMultipartFile gameFile = new MockMultipartFile(
                "file",
                "quiz.game.throne",
                "application/octet-stream",
                bytes.toByteArray()
        );

        ResponseEntity<String> response = fixture.controller().uploadGame(
                gameFile,
                LOBBY_CODE,
                user("host")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(fixture.lobby().hasLoadedGame());
        assertFalse(Files.exists(fixture.storageDir().resolve(LOBBY_CODE)));
    }
}
