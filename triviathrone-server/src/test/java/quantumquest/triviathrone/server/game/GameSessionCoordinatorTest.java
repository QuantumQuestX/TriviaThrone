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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameSessionCoordinatorTest {
    private static final String LOBBY_CODE = "ROOM01";

    @Mock
    private GameEventService gameEventService;

    @Mock
    private SystemModManager systemModManager;

    @Mock
    private IGameDefinition gameDefinition;

    @Mock
    private IGameSession session;

    private Lobby lobby;
    private GameSessionCoordinator coordinator;

    @BeforeEach
    void setUp() {
        lobby = new Lobby(LOBBY_CODE, "Test lobby", "host");
        lobby.addUser("player");
        lobby.loadGame(gameDefinition);
        lenient().when(gameDefinition.gameType()).thenReturn("test-game");
        when(gameDefinition.createSession(LOBBY_CODE, lobby.listUsernames())).thenReturn(session);
        lenient().when(session.handleAction(any(), anyString(), anyBoolean())).thenReturn(GameActionResult.allow());
        lobby.startGame();
        coordinator = new GameSessionCoordinator(gameEventService, systemModManager);
    }

    @AfterEach
    void closeLobby() {
        lobby.endGame();
    }

    @ParameterizedTest
    @MethodSource("gameActions")
    void broadcastsExactlyOneStateUpdateForEachAcceptedGameAction(String actionName, String username, boolean isHost) {
        GameActionMessage action = new GameActionMessage(LOBBY_CODE, actionName, Map.of());

        coordinator.handleAction(action, lobby, username);

        verify(session).handleAction(action, username, isHost);
        verify(gameEventService, times(1)).broadcastState(LOBBY_CODE, session);
    }

    private static Stream<Arguments> gameActions() {
        return Stream.of(
                Arguments.of("SELECT_ROUND", "host", true),
                Arguments.of("SELECT_QUESTION", "host", true),
                Arguments.of("PLAY_MEDIA", "host", true),
                Arguments.of("OPEN_BUZZ", "host", true),
                Arguments.of("BUZZ", "player", false),
                Arguments.of("CLOSE_BUZZ", "host", true),
                Arguments.of("MARK_CORRECT", "host", true),
                Arguments.of("MARK_INCORRECT", "host", true),
                Arguments.of("RESET", "host", true)
        );
    }

    @Test
    void rejectedGameActionsDoNotBroadcastState() {
        GameActionMessage action = new GameActionMessage(LOBBY_CODE, "PLAY_MEDIA", Map.of());
        when(session.handleAction(action, "player", false)).thenReturn(GameActionResult.rejected());

        coordinator.handleAction(action, lobby, "player");

        verify(gameEventService, never()).broadcastState(eq(LOBBY_CODE), eq(session));
    }

    @Test
    void hostBroadcastsFinalMessageBeforeEndingGame() {
        coordinator.handleAction(
                new GameActionMessage(LOBBY_CODE, GameControlAction.END_GAME.wireName(), Map.of()),
                lobby,
                "host"
        );

        verify(gameEventService).broadcastGameEnded(LOBBY_CODE, session, "host");
        verify(systemModManager).publishEvent(isA(GameEndedEvent.class));
        assertFalse(lobby.hasActiveGame());
    }

    @Test
    void doesNotEndAReplacementSessionIfTheLobbyChangesDuringTheEndedBroadcast() {
        IGameSession replacementSession = mock(IGameSession.class);
        when(gameDefinition.createSession(LOBBY_CODE, lobby.listUsernames())).thenReturn(replacementSession);
        doAnswer(invocation -> {
            assertSame(gameEventService, invocation.getMock());
            lobby.endGame();
            lobby.startGame();
            return null;
        }).when(gameEventService).broadcastGameEnded(LOBBY_CODE, session, "host");

        coordinator.handleAction(
                new GameActionMessage(LOBBY_CODE, GameControlAction.END_GAME.wireName(), Map.of()),
                lobby,
                "host"
        );

        assertSame(replacementSession, lobby.getGameSession());
        verify(replacementSession, never()).close();
        verify(systemModManager, never()).publishEvent(isA(GameEndedEvent.class));
    }

    @Test
    void ignoresAnEndedActionFromAReplacedSession() {
        GameActionMessage action = new GameActionMessage(LOBBY_CODE, "RESET", Map.of());
        IGameSession replacementSession = mock(IGameSession.class);
        when(gameDefinition.createSession(LOBBY_CODE, lobby.listUsernames())).thenReturn(replacementSession);
        doAnswer(invocation -> {
            assertSame(session, invocation.getMock());
            lobby.endGame();
            lobby.startGame();
            return GameActionResult.endGame();
        }).when(session).handleAction(action, "host", true);

        coordinator.handleAction(action, lobby, "host");

        assertSame(replacementSession, lobby.getGameSession());
        verify(gameEventService, never()).broadcastGameEnded(LOBBY_CODE, session, "host");
        verify(replacementSession, never()).close();
        verify(systemModManager, never()).publishEvent(isA(GameEndedEvent.class));
    }

    @Test
    void gameCanRequestItsOwnEndAfterAnAction() {
        GameActionMessage action = new GameActionMessage(LOBBY_CODE, "RESET", Map.of());
        when(session.handleAction(action, "host", true)).thenReturn(GameActionResult.endGame());

        coordinator.handleAction(action, lobby, "host");

        verify(gameEventService).broadcastGameEnded(LOBBY_CODE, session, "host");
        verify(systemModManager).publishEvent(isA(GameEndedEvent.class));
        verify(gameEventService, never()).broadcastState(LOBBY_CODE, session);
        assertFalse(lobby.hasActiveGame());
    }

    @Test
    void publishesGameStartedOnlyOnTheTransitionToActive() {
        lobby.endGame();
        GameActionMessage start = new GameActionMessage(
                LOBBY_CODE, GameControlAction.START_GAME.wireName(), Map.of()
        );

        coordinator.handleAction(start, lobby, "host");
        coordinator.handleAction(start, lobby, "host");

        verify(systemModManager, times(1)).publishEvent(isA(GameStartedEvent.class));
        verify(gameEventService, times(2)).broadcastState(LOBBY_CODE, session);
    }

    @Test
    void broadcastsStateAfterAnAsynchronousSessionChange() {
        lobby.endGame();
        ArgumentCaptor<Runnable> listener = ArgumentCaptor.forClass(Runnable.class);
        coordinator.handleAction(
                new GameActionMessage(LOBBY_CODE, GameControlAction.START_GAME.wireName(), Map.of()),
                lobby,
                "host"
        );
        verify(session).setStateChangeListener(listener.capture());
        clearInvocations(gameEventService);

        listener.getValue().run();

        verify(gameEventService).broadcastState(LOBBY_CODE, session);
    }

    @Test
    void ignoresAsynchronousChangesFromAnEndedSession() {
        lobby.endGame();
        ArgumentCaptor<Runnable> listener = ArgumentCaptor.forClass(Runnable.class);
        coordinator.handleAction(
                new GameActionMessage(LOBBY_CODE, GameControlAction.START_GAME.wireName(), Map.of()),
                lobby,
                "host"
        );
        verify(session).setStateChangeListener(listener.capture());
        lobby.endGame();
        clearInvocations(gameEventService);

        listener.getValue().run();

        verify(gameEventService, never()).broadcastState(LOBBY_CODE, session);
    }

    @Test
    void ignoresAsynchronousChangesWhileTheSessionIsClosing() {
        lobby.endGame();
        ArgumentCaptor<Runnable> listener = ArgumentCaptor.forClass(Runnable.class);
        coordinator.handleAction(
                new GameActionMessage(LOBBY_CODE, GameControlAction.START_GAME.wireName(), Map.of()),
                lobby,
                "host"
        );
        verify(session).setStateChangeListener(listener.capture());
        clearInvocations(gameEventService);
        doAnswer(invocation -> {
            assertSame(session, invocation.getMock());
            listener.getValue().run();
            return null;
        }).when(session).close();

        lobby.endGame();

        verify(gameEventService, never()).broadcastState(LOBBY_CODE, session);
    }

    @Test
    void syncBroadcastsStateWithoutRunningAGameAction() {
        coordinator.handleAction(
                new GameActionMessage(LOBBY_CODE, GameControlAction.SYNC.wireName(), Map.of()),
                lobby,
                "player"
        );

        verify(gameEventService).broadcastState(LOBBY_CODE, session);
        verify(session, never()).handleAction(any(), anyString(), anyBoolean());
    }

    @Test
    void rejectsControlActionsWithUnexpectedParameters() {
        GameActionMessage start = new GameActionMessage(
                LOBBY_CODE,
                GameControlAction.START_GAME.wireName(),
                Map.of("unexpected", true)
        );

        coordinator.handleAction(start, lobby, "host");

        verify(session, never()).handleAction(start, "host", true);
        verify(gameEventService, never()).broadcastState(LOBBY_CODE, session);
    }

    @Test
    void ignoresActionsFromNonMembers() {
        GameActionMessage action = new GameActionMessage(LOBBY_CODE, "PLAY_MEDIA", Map.of());

        coordinator.handleAction(action, lobby, "outsider");

        verify(session, never()).handleAction(any(), anyString(), anyBoolean());
        verify(gameEventService, never()).broadcastState(LOBBY_CODE, session);
    }
}
