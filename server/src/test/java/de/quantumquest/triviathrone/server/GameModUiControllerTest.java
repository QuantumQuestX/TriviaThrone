package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.api.game.GameUiDescriptor;
import de.quantumquest.triviathrone.api.game.IGameDefinition;
import de.quantumquest.triviathrone.api.game.IGameProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GameModUiControllerTest {
    private final LobbyService lobbies = mock(LobbyService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new GameModUiController(new GameProviderRegistry(List.of(new TestGameProvider())), lobbies)
        ).build();
    }

    @Test
    void servesModEditorAndUiAssetsFromItsClassLoader() throws Exception {
        mockMvc.perform(get("/editor"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/game-creator"));

        mockMvc.perform(get("/games/sample/editor"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("Game mod editor")));

        mockMvc.perform(get("/games/sample/play"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Game mod game")));

        mockMvc.perform(get("/mods/sample/ui/js/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("mod UI asset")));

        mockMvc.perform(get("/plugins/sample/ui/js/app.js"))
                .andExpect(status().isOk());
    }

    @Test
    void servesGamePageForTheLobbyGameType() throws Exception {
        IGameDefinition definition = mock(IGameDefinition.class);
        when(definition.gameType()).thenReturn("sample");
        Lobby lobby = new Lobby("ROOM01", "Test lobby", "host");
        lobby.loadGame(definition);
        when(lobbies.getLobby("ROOM01")).thenReturn(Optional.of(lobby));

        mockMvc.perform(get("/game").param("code", "ROOM01"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/games/sample/play?code=ROOM01"));
    }

    @Test
    void rejectsPathTraversalInGameModAssetRequests() throws Exception {
        mockMvc.perform(get("/mods/sample/ui/%2e%2e%2fmod.xml"))
                .andExpect(status().isNotFound());
    }

    private static final class TestGameProvider implements IGameProvider {
        @Override
        public String gameType() {
            return "sample";
        }

        @Override
        public boolean supportsFile(String filename) {
            return false;
        }

        @Override
        public Optional<IGameDefinition> load(Path gameDirectory) {
            return Optional.empty();
        }

        @Override
        public Optional<GameUiDescriptor> uiDescriptor() {
            return Optional.of(new GameUiDescriptor("editor.html", "game.html"));
        }
    }
}
