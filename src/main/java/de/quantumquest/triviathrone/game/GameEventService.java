package de.quantumquest.triviathrone.game;

import de.quantumquest.triviathrone.dto.game.GameStateMessage;
import de.quantumquest.triviathrone.dto.game.GameEndedMessage;
import de.quantumquest.triviathrone.dto.game.GameStateMessage.QuestionTileMessage;
import de.quantumquest.triviathrone.dto.game.GameStateMessage.RoundBoardMessage;
import de.quantumquest.triviathrone.game.definition.Category;
import de.quantumquest.triviathrone.game.definition.QuestionDefinition;
import de.quantumquest.triviathrone.game.definition.Round;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.IntStream;

@Service
public class GameEventService {
    private final SimpMessagingTemplate messagingTemplate;

    public GameEventService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = Objects.requireNonNull(messagingTemplate, "messagingTemplate");
    }

    public void broadcastState(String lobbyCode, GameSession session) {
        if (session == null) return;

        GameDefinition definition = session.getDefinition();
        List<RoundBoardMessage> rounds = IntStream.range(0, definition.rounds().size())
                .mapToObj(roundIdx -> {
                    Round round = definition.rounds().get(roundIdx);
                    List<String> categories = round.categories().stream()
                            .map(Category::name)
                            .toList();
                    int questionCount = round.categories().stream()
                            .mapToInt(category -> category.questions().size())
                            .max()
                            .orElse(0);
                    List<List<QuestionTileMessage>> questions = IntStream.range(0, questionCount)
                            .mapToObj(questionIdx -> IntStream.range(0, round.categories().size())
                                    .mapToObj(categoryIdx -> {
                                        QuestionDefinition question = round.categories()
                                                .get(categoryIdx)
                                                .questions()
                                                .get(questionIdx);
                                        return new QuestionTileMessage(
                                                question.points(),
                                                session.isQuestionUsed(roundIdx, categoryIdx, questionIdx)
                                        );
                                    })
                                    .toList())
                            .toList();
                    return new RoundBoardMessage(round.name(), categories, questions);
                })
                .toList();
        int roundIdx = session.getCurrentRoundIdx();
        String roundName = roundIdx >= 0 && roundIdx < definition.rounds().size()
                ? definition.rounds().get(roundIdx).name()
                : null;

        GameStateMessage message = new GameStateMessage(
                lobbyCode,
                session.getCurrentState(),
                session.getBuzzWinner(),
                session.getScores(),
                session.getCurrentRoundIdx(),
                session.getCurrentCategoryIdx(),
                session.getCurrentQuestionIdx(),
                definition.title(),
                rounds,
                roundName,
                session.getCurrentQuestionMessage(),
                session.getBuzzedPlayers(),
                session.getMediaPlaybackToken(),
                session.getMediaPauseToken()
        );

        messagingTemplate.convertAndSend(
                "/topic/game." + lobbyCode,
                message
        );
    }

    public void broadcastGameEnded(String lobbyCode, GameSession session, String hostUsername) {
        List<GameEndedMessage.LeaderboardEntry> leaderboard = session.getScores().entrySet().stream()
                .filter(entry -> !entry.getKey().equals(hostUsername))
                .sorted(Comparator.comparingInt((Map.Entry<String, Integer> entry) -> entry.getValue())
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> new GameEndedMessage.LeaderboardEntry(entry.getKey(), entry.getValue()))
                .toList();

        messagingTemplate.convertAndSend(
                "/topic/game." + lobbyCode,
                new GameEndedMessage(lobbyCode, true, leaderboard)
        );
    }
}
