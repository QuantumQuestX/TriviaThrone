package quantumquest.triviathrone.server.game;

import quantumquest.triviathrone.dto.game.GameActionMessage;
import quantumquest.triviathrone.server.Lobby;
import quantumquest.triviathrone.server.LobbyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameWebSocketControllerTest {
    private static final String LOBBY_CODE = "ROOM01";

    @Mock
    private LobbyService lobbyService;

    @Mock
    private GameSessionCoordinator gameSessionCoordinator;

    @Mock
    private Lobby lobby;

    private GameWebSocketController controller;

    @BeforeEach
    void setUp() {
        controller = new GameWebSocketController(lobbyService, gameSessionCoordinator);
    }

    @Test
    void delegatesLobbyActionsToCoordinator() {
        GameActionMessage action = new GameActionMessage(LOBBY_CODE, "SELECT_QUESTION", Map.of());
        when(lobbyService.getLobby(LOBBY_CODE)).thenReturn(Optional.of(lobby));

        controller.handleAction(action, () -> "player");

        verify(gameSessionCoordinator).handleAction(action, lobby, "player");
    }
}
