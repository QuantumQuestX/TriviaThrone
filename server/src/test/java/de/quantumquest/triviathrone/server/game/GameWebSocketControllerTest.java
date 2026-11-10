package de.quantumquest.triviathrone.server.game;

import de.quantumquest.triviathrone.api.game.GameActionResult;
import de.quantumquest.triviathrone.api.game.IGameDefinition;
import de.quantumquest.triviathrone.api.game.IGameSession;
import de.quantumquest.triviathrone.dto.game.GameActionMessage;
import de.quantumquest.triviathrone.server.Lobby;
import de.quantumquest.triviathrone.server.LobbyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameWebSocketControllerTest {
    private static final String LOBBY_CODE = "ROOM01";

    @Mock
    private LobbyService lobbyService;

    @Mock
    private GameEventService gameEventService;

    @Mock
    private IGameDefinition gameDefinition;

    @Mock
    private IGameSession session;

    private Lobby lobby;
    private GameWebSocketController controller;

    @BeforeEach
    void setUp() {
        lobby = new Lobby(LOBBY_CODE, "Test lobby", "host");
        lobby.addUser("player");
        lobby.loadGame(gameDefinition);
        when(gameDefinition.createSession(LOBBY_CODE, lobby.listUsernames())).thenReturn(session);
        lenient().when(session.handleAction(any(), anyString(), anyBoolean())).thenReturn(GameActionResult.allow());
        lobby.startGame();

        when(lobbyService.getLobby(LOBBY_CODE)).thenReturn(Optional.of(lobby));
        controller = new GameWebSocketController(lobbyService, gameEventService);
    }

    @AfterEach
    void closeLobby() {
        lobby.endGame();
    }

    @Test
    void forwardsHostActionsToTheActiveGame() {
        GameActionMessage action = new GameActionMessage(LOBBY_CODE, "PLAY_MEDIA", Map.of());
        controller.handleAction(action, principal("host"));

        verify(session).handleAction(action, "host", true);
        verify(gameEventService).broadcastState(LOBBY_CODE, session);
    }

    @Test
    void rejectedGameActionsAreNotBroadcast() {
        GameActionMessage action = new GameActionMessage(LOBBY_CODE, "PLAY_MEDIA", Map.of());
        when(session.handleAction(action, "player", false)).thenReturn(GameActionResult.rejected());

        controller.handleAction(action, principal("player"));

        verify(gameEventService, never()).broadcastState(eq(LOBBY_CODE), eq(session));
    }

    @Test
    void hostBroadcastsFinalMessageBeforeEndingGame() {
        controller.handleAction(new GameActionMessage(LOBBY_CODE, "END_GAME", Map.of()), principal("host"));

        verify(gameEventService).broadcastGameEnded(LOBBY_CODE, session, "host");
        assertFalse(lobby.hasActiveGame());
    }

    @Test
    void gameCanRequestItsOwnEndAfterAnAction() {
        GameActionMessage action = new GameActionMessage(LOBBY_CODE, "RESET", Map.of());
        when(session.handleAction(action, "host", true)).thenReturn(GameActionResult.endGame());

        controller.handleAction(action, principal("host"));

        verify(gameEventService).broadcastGameEnded(LOBBY_CODE, session, "host");
        assertFalse(lobby.hasActiveGame());
    }

    private static Principal principal(String username) {
        return () -> username;
    }
}
