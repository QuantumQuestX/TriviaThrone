package de.quantumquest.triviathrone.game;

import de.quantumquest.triviathrone.dto.game.GameActionMessage;
import de.quantumquest.triviathrone.game.definition.Category;
import de.quantumquest.triviathrone.game.definition.MediaType;
import de.quantumquest.triviathrone.game.definition.QuestionDefinition;
import de.quantumquest.triviathrone.game.definition.Round;
import de.quantumquest.triviathrone.server.Lobby;
import de.quantumquest.triviathrone.server.LobbyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameWebSocketControllerTest {
    private static final String LOBBY_CODE = "ROOM01";

    @Mock
    private LobbyService lobbyService;

    @Mock
    private GameEventService gameEventService;

    private Lobby lobby;
    private GameSession session;
    private GameWebSocketController controller;

    @BeforeEach
    void setUp() {
        lobby = new Lobby(LOBBY_CODE, "Test lobby", "host");
        lobby.addUser("player");
        lobby.loadGame(new GameDefinition(
                "Test game",
                "Description",
                List.of(new Round("Round 1", List.of(
                        new Category("Category", List.of(new QuestionDefinition(
                                100,
                                "Prompt",
                                "resources/clue.mp4",
                                MediaType.VIDEO
                        )))
                )))
        ));
        lobby.startGame();
        session = lobby.getGameSession();
        session.selectQuestion(0, 0, 0);

        when(lobbyService.getLobby(LOBBY_CODE)).thenReturn(Optional.of(lobby));
        controller = new GameWebSocketController(lobbyService, gameEventService);
    }

    @AfterEach
    void closeLobby() {
        lobby.endGame();
    }

    @Test
    void hostCanStartMediaAndOpenBuzzer() {
        controller.handleAction(
                new GameActionMessage(LOBBY_CODE, GameActionMessage.ActionType.PLAY_MEDIA, null, null, null),
                principal("host")
        );

        assertEquals(GameSession.State.BUZZ_OPEN, session.getCurrentState());
        verify(gameEventService).broadcastState(LOBBY_CODE, session);
    }

    @Test
    void nonHostCannotStartMedia() {
        controller.handleAction(
                new GameActionMessage(LOBBY_CODE, GameActionMessage.ActionType.PLAY_MEDIA, null, null, null),
                principal("player")
        );

        assertEquals(GameSession.State.QUESTION_SELECTED, session.getCurrentState());
        verify(gameEventService, never()).broadcastState(eq(LOBBY_CODE), eq(session));
    }

    private static Principal principal(String username) {
        return () -> username;
    }
}
