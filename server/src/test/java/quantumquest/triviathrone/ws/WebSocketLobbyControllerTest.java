package quantumquest.triviathrone.ws;

import quantumquest.triviathrone.dto.lobby.LobbyCommonMessage;
import quantumquest.triviathrone.mod.GameModManager;
import quantumquest.triviathrone.server.GameProviderRegistry;
import quantumquest.triviathrone.server.Lobby;
import quantumquest.triviathrone.server.LobbyEventService;
import quantumquest.triviathrone.server.LobbyGameFiles;
import quantumquest.triviathrone.server.LobbyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketLobbyControllerTest {
    private static final String LOBBY_CODE = "ROOM01";

    @TempDir
    Path tempDir;

    @Test
    void deletesUploadedGameFilesOnlyAfterLastUserLeaves() throws IOException {
        try (GameModManager gameModManager = GameModManager.load(tempDir.resolve("mods/games"))) {
            LobbyService lobbyService = mock(LobbyService.class);
            LobbyEventService lobbyEventService = mock(LobbyEventService.class);
            LobbyGameFiles lobbyGameFiles = new LobbyGameFiles(
                    tempDir.resolve("games").toString(),
                    new GameProviderRegistry(gameModManager)
            );
            WebSocketLobbyController controller =
                    new WebSocketLobbyController(lobbyService, lobbyEventService, lobbyGameFiles);
            Lobby lobby = new Lobby(LOBBY_CODE, "Test lobby", "host");
            lobby.addUser("player");
            when(lobbyService.getLobby(LOBBY_CODE)).thenReturn(Optional.of(lobby));

            Path gameDirectory = tempDir.resolve("games").resolve(LOBBY_CODE);
            Path uploadedGame = gameDirectory.resolve("resources/clue.txt");
            Files.createDirectories(uploadedGame.getParent());
            Files.writeString(uploadedGame, "clue data");

            controller.leave(new LobbyCommonMessage("Test lobby", LOBBY_CODE), principal("player"));
            assertTrue(Files.exists(gameDirectory));

            controller.leave(new LobbyCommonMessage("Test lobby", LOBBY_CODE), principal("host"));

            assertFalse(Files.exists(gameDirectory));
            verify(lobbyService).removeLobby(LOBBY_CODE);
        }
    }

    private static Principal principal(String username) {
        return () -> username;
    }
}
