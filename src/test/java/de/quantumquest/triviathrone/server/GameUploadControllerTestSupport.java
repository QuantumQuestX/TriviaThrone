package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.auth.UserAccount;
import de.quantumquest.triviathrone.game.GameDefinition;
import de.quantumquest.triviathrone.game.definition.Category;
import de.quantumquest.triviathrone.game.definition.MediaType;
import de.quantumquest.triviathrone.game.definition.QuestionDefinition;
import de.quantumquest.triviathrone.game.definition.Round;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class GameUploadControllerTestSupport {
    static final String LOBBY_CODE = "ROOM01";

    private GameUploadControllerTestSupport() {
    }

    static Fixture createFixture(Path tempDir) {
        LobbyService lobbyService = mock(LobbyService.class);
        LobbyEventService lobbyEventService = mock(LobbyEventService.class);
        Path storageDir = tempDir.resolve("games");
        GameUploadController controller = new GameUploadController(
                lobbyService,
                lobbyEventService,
                new LobbyGameFiles(storageDir.toString())
        );
        Lobby lobby = new Lobby(LOBBY_CODE, "Test lobby", "host");
        lobby.addUser("player");
        when(lobbyService.getLobby(LOBBY_CODE)).thenReturn(Optional.of(lobby));
        return new Fixture(lobbyEventService, controller, lobby, storageDir);
    }

    static GameDefinition definition(String mediaPath) {
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

    static UserAccount user(String username) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        return user;
    }

    static byte[] gameArchive() throws IOException {
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

    record Fixture(
            LobbyEventService lobbyEventService,
            GameUploadController controller,
            Lobby lobby,
            Path storageDir
    ) {
    }
}
