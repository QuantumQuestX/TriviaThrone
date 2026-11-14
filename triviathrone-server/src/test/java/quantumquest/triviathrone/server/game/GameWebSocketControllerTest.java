package quantumquest.triviathrone.server.game;

import quantumquest.triviathrone.api.game.GameActionResult;
import quantumquest.triviathrone.api.game.GameControlAction;
import quantumquest.triviathrone.api.game.IGameDefinition;
import quantumquest.triviathrone.api.game.IGameSession;
import quantumquest.triviathrone.api.system.events.GameEndedEvent;
import quantumquest.triviathrone.api.system.events.GameStartedEvent;
import quantumquest.triviathrone.dto.game.GameActionMessage;
import quantumquest.triviathrone.mod.SystemModManager;
import quantumquest.triviathrone.server.Lobby;
import quantumquest.triviathrone.server.LobbyService;
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
import static org.mockito.ArgumentMatchers.isA;
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
    private SystemModManager systemModManager;

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
        lenient().when(gameDefinition.gameType()).thenReturn("test-game");
        when(gameDefinition.createSession(LOBBY_CODE, lobby.listUsernames())).thenReturn(session);
        lenient().when(session.handleAction(any(), anyString(), anyBoolean())).thenReturn(GameActionResult.allow());
        lobby.startGame();

        when(lobbyService.getLobby(LOBBY_CODE)).thenReturn(Optional.of(lobby));
        controller = new GameWebSocketController(lobbyService, gameEventService, systemModManager);
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
        controller.handleAction(
                new GameActionMessage(LOBBY_CODE, GameControlAction.END_GAME.wireName(), Map.of()),
                principal("host")
        );

        verify(gameEventService).broadcastGameEnded(LOBBY_CODE, session, "host");
        verify(systemModManager).publishEvent(isA(GameEndedEvent.class));
        assertFalse(lobby.hasActiveGame());
    }

    @Test
    void gameCanRequestItsOwnEndAfterAnAction() {
        GameActionMessage action = new GameActionMessage(LOBBY_CODE, "RESET", Map.of());
        when(session.handleAction(action, "host", true)).thenReturn(GameActionResult.endGame());

        controller.handleAction(action, principal("host"));

        verify(gameEventService).broadcastGameEnded(LOBBY_CODE, session, "host");
        verify(systemModManager).publishEvent(isA(GameEndedEvent.class));
        assertFalse(lobby.hasActiveGame());
    }

    @Test
    void publishesGameStartedOnlyOnTheTransitionToActive() {
        lobby.endGame();
        GameActionMessage start = new GameActionMessage(
                LOBBY_CODE, GameControlAction.START_GAME.wireName(), Map.of()
        );

        controller.handleAction(start, principal("host"));
        controller.handleAction(start, principal("host"));

        verify(systemModManager).publishEvent(isA(GameStartedEvent.class));
    }

    @Test
    void rejectsControlActionsWithUnexpectedParameters() {
        GameActionMessage start = new GameActionMessage(
                LOBBY_CODE,
                GameControlAction.START_GAME.wireName(),
                Map.of("unexpected", true)
        );

        controller.handleAction(start, principal("host"));

        verify(session, never()).handleAction(start, "host", true);
        verify(gameEventService, never()).broadcastState(LOBBY_CODE, session);
    }

    private static Principal principal(String username) {
        return () -> username;
    }
}
