package de.quantumquest.triviathrone.server.game;

import de.quantumquest.triviathrone.api.game.IGameSession;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class GameEventService {
    private final SimpMessagingTemplate messagingTemplate;

    public GameEventService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = Objects.requireNonNull(messagingTemplate, "messagingTemplate");
    }

    public void broadcastState(String lobbyCode, IGameSession session) {
        if (session == null) return;
        messagingTemplate.convertAndSend("/topic/game." + lobbyCode, session.stateMessage());
    }

    public void broadcastGameEnded(String lobbyCode, IGameSession session, String hostUsername) {
        messagingTemplate.convertAndSend(
                "/topic/game." + lobbyCode,
                session.endedMessage(hostUsername)
        );
    }
}
