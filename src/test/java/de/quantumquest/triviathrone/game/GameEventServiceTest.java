package de.quantumquest.triviathrone.game;

import de.quantumquest.triviathrone.dto.game.GameEndedMessage;
import de.quantumquest.triviathrone.dto.game.GameStateMessage;
import de.quantumquest.triviathrone.game.definition.Category;
import de.quantumquest.triviathrone.game.definition.MediaType;
import de.quantumquest.triviathrone.game.definition.QuestionDefinition;
import de.quantumquest.triviathrone.game.definition.Round;
import de.quantumquest.triviathrone.server.Lobby;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GameEventServiceTest {
    private GameSession session;

    @AfterEach
    void closeSession() {
        if (session != null) session.close();
    }

    @Test
    void broadcastsMediaPlaybackAndPauseTokens() {
        Lobby lobby = new Lobby("ROOM01", "Test lobby", "host");
        lobby.addUser("player");
        session = new GameSession(lobby, new GameDefinition(
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
        session.selectQuestion(0, 0, 0);
        session.playMedia();
        session.registerBuzz("player");
        session.closeBuzz();

        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        new GameEventService(messagingTemplate).broadcastState("ROOM01", session);

        ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game.ROOM01"), messageCaptor.capture());
        GameStateMessage state = (GameStateMessage) messageCaptor.getValue();
        assertEquals(GameSession.State.BUZZ_LOCKED, state.state());
        assertNotNull(state.mediaPlaybackToken());
        assertNotNull(state.mediaPauseToken());
    }

    @Test
    void broadcastsFinalLeaderboardRankedByScoreWithoutHost() {
        Lobby lobby = new Lobby("ROOM01", "Test lobby", "host");
        lobby.addUser("alice");
        lobby.addUser("bob");
        session = new GameSession(lobby, new GameDefinition(
                "Test game",
                "Description",
                List.of(new Round("Round 1", List.of(
                        new Category("Category", List.of(new QuestionDefinition(
                                100,
                                "Prompt",
                                null,
                                MediaType.UNDEFINED
                        )))
                )))
        ));
        session.selectQuestion(0, 0, 0);
        session.openBuzz();
        session.registerBuzz("bob");
        session.closeBuzz();
        session.markCorrect();

        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        new GameEventService(messagingTemplate).broadcastGameEnded("ROOM01", session, "host");

        ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game.ROOM01"), messageCaptor.capture());
        GameEndedMessage message = (GameEndedMessage) messageCaptor.getValue();
        assertTrue(message.gameEnded());
        assertEquals(List.of(
                new GameEndedMessage.LeaderboardEntry("bob", 100),
                new GameEndedMessage.LeaderboardEntry("alice", 0)
        ), message.leaderboard());
    }
}
