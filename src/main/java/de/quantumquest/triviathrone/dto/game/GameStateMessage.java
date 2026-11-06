package de.quantumquest.triviathrone.dto.game;

import de.quantumquest.triviathrone.game.GameSession;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record GameStateMessage(String lobbyCode, GameSession.State state, String buzzWinner,
                               Map<String, Integer> scores,
                               Integer curRoundIdx, Integer curCatIdx, Integer curQuestionIdx,
                               String gameTitle, List<RoundBoardMessage> rounds, String roundName,
                               GameQuestionMessage question, Set<String> buzzedPlayers,
                               String mediaPlaybackToken, String mediaPauseToken) {
    public GameStateMessage {
        scores = Map.copyOf(scores);
        rounds = List.copyOf(rounds);
        buzzedPlayers = Set.copyOf(buzzedPlayers);
    }

    public record RoundBoardMessage(String name, List<String> categories, List<List<QuestionTileMessage>> questions) {
        public RoundBoardMessage {
            categories = List.copyOf(categories);
            questions = questions.stream().map(List::copyOf).toList();
        }
    }

    public record QuestionTileMessage(int points, boolean used) {
    }
}
