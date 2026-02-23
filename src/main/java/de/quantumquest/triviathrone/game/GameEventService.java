package de.quantumquest.triviathrone.game;

import de.quantumquest.triviathrone.dto.GameStateMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class GameEventService {
    private final SimpMessagingTemplate messagingTemplate;

    public GameEventService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastState(String lobbyCode, GameSession session) {
        if (session == null) return;

        GameStateMessage message = new GameStateMessage(
                lobbyCode,
                session.getCurrentState(),
                session.getBuzzWinner(),
                session.getScores(),
                session.getCurrentRoundIdx(),
                session.getCurrentQuestionIdx(),
                session.getCurrentRoundIdx()
        );

        messagingTemplate.convertAndSend(
                "/topic/game." + lobbyCode,
                message
        );
    }
}
