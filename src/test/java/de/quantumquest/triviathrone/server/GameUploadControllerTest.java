package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.auth.UserAccount;
import de.quantumquest.triviathrone.game.GameDefinition;
import de.quantumquest.triviathrone.game.definition.Category;
import de.quantumquest.triviathrone.game.definition.MediaType;
import de.quantumquest.triviathrone.game.definition.QuestionDefinition;
import de.quantumquest.triviathrone.game.definition.Round;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameUploadControllerTest {
    private static final String LOBBY_CODE = "ROOM01";

    @TempDir
    Path tempDir;

    private LobbyService lobbyService;
    private LobbyEventService lobbyEventService;
    private GameUploadController controller;
    private Lobby lobby;
    private Path storageDir;

    @BeforeEach
    void setUp() {
        lobbyService = mock(LobbyService.class);
        lobbyEventService = mock(LobbyEventService.class);
        storageDir = tempDir.resolve("games");
        controller = new GameUploadController(
                lobbyService,
                lobbyEventService,
                storageDir.toString()
        );
        lobby = new Lobby(LOBBY_CODE, "Test lobby", "host");
        lobby.addUser("player");
        when(lobbyService.getLobby(LOBBY_CODE)).thenReturn(Optional.of(lobby));
    }

    @Test
    void lobbyMemberCanLoadMediaReferencedByGame() throws IOException {
        lobby.loadGame(definition("resources/clue.txt"));
        Path mediaFile = storageDir.resolve(LOBBY_CODE).resolve("resources/clue.txt");
        Files.createDirectories(mediaFile.getParent());
        Files.writeString(mediaFile, "test media");

        ResponseEntity<Resource> response = controller.getGameMedia(
                LOBBY_CODE,
                "resources/clue.txt",
                user("player")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        try (var content = response.getBody().getInputStream()) {
            assertEquals("test media", new String(content.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void nonMemberCannotLoadLobbyMedia() {
        lobby.loadGame(definition("resources/clue.txt"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getGameMedia(LOBBY_CODE, "resources/clue.txt", user("outsider"))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void rejectsPathsOutsideLobbyDirectory() {
        lobby.loadGame(definition("../outside.txt"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getGameMedia(LOBBY_CODE, "../outside.txt", user("player"))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void rejectsFilesNotReferencedByGame() {
        lobby.loadGame(definition("resources/clue.txt"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getGameMedia(LOBBY_CODE, "resources/other.txt", user("player"))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void uploadsGameIntoConfiguredStorageDirectory() throws IOException {
        MockMultipartFile gameFile = new MockMultipartFile(
                "file",
                "quiz.game.throne",
                "application/octet-stream",
                gameArchive()
        );

        ResponseEntity<String> response = controller.uploadGame(gameFile, LOBBY_CODE, user("host"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(lobby.hasLoadedGame());
        assertTrue(Files.isRegularFile(storageDir.resolve(LOBBY_CODE).resolve("resources/clue.txt")));
        assertFalse(lobby.getGameDefinition().rounds().isEmpty());
        verify(lobbyEventService).hostUploadedGame(LOBBY_CODE, "host", "quiz.game.throne");
    }

    private static GameDefinition definition(String mediaPath) {
        return new GameDefinition(
                "Test game",
                "Description",
                List.of(new Round("Round 1", List.of(
                        new Category("Category", List.of(new QuestionDefinition(
                                100,
                                "Prompt",
                                mediaPath,
                                MediaType.UNDEFINED
                        )))
                )))
        );
    }

    private static UserAccount user(String username) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        return user;
    }

    private static byte[] gameArchive() throws IOException {
        String manifest = """
                <game>
                    <meta><title>Uploaded game</title><description>Test</description></meta>
                    <resources><resource id="clue" file="resources/clue.txt"/></resources>
                    <rounds>
                        <round name="Round 1" categoryCount="1" questionCount="1">
                            <categories><category>Category</category></categories>
                            <questions>
                                <question row="0" category="0" points="100">
                                    <prompt>Prompt</prompt>
                                    <media ref="clue"/>
                                </question>
                            </questions>
                        </round>
                    </rounds>
                </game>
                """;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream archive = new ZipOutputStream(bytes)) {
            archive.putNextEntry(new ZipEntry("manifest.xml"));
            archive.write(manifest.getBytes(StandardCharsets.UTF_8));
            archive.closeEntry();
            archive.putNextEntry(new ZipEntry("resources/clue.txt"));
            archive.write("clue data".getBytes(StandardCharsets.UTF_8));
            archive.closeEntry();
        }
        return bytes.toByteArray();
    }
}
