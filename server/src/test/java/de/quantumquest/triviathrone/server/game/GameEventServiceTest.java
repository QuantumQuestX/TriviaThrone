package de.quantumquest.triviathrone.server.game;

import de.quantumquest.triviathrone.api.game.IGameMessage;
import de.quantumquest.triviathrone.api.game.IGameSession;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameEventServiceTest {
    @Test
    void broadcastsStateProducedByTheGameImplementation() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        IGameSession session = mock(IGameSession.class);
        IGameMessage state = mock(IGameMessage.class);
        when(session.stateMessage()).thenReturn(state);

        new GameEventService(messagingTemplate).broadcastState("ROOM01", session);

        ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game.ROOM01"), messageCaptor.capture());
        assertSame(state, messageCaptor.getValue());
    }

    @Test
    void broadcastsEndMessageProducedByTheGameImplementation() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        IGameSession session = mock(IGameSession.class);
        IGameMessage endMessage = mock(IGameMessage.class);
        when(session.endedMessage("host")).thenReturn(endMessage);

        new GameEventService(messagingTemplate).broadcastGameEnded("ROOM01", session, "host");

        verify(messagingTemplate).convertAndSend("/topic/game.ROOM01", endMessage);
    }
}
