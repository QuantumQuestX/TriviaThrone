package de.quantumquest.triviathrone.games.jeopardy;

import de.quantumquest.triviathrone.api.game.GameActionResult;
import de.quantumquest.triviathrone.api.game.IGameMessage;
import de.quantumquest.triviathrone.api.game.IGameSession;
import de.quantumquest.triviathrone.dto.game.GameActionMessage;
import de.quantumquest.triviathrone.games.jeopardy.dto.JeopardyEndedMessage;
import de.quantumquest.triviathrone.games.jeopardy.dto.GameQuestionMessage;
import de.quantumquest.triviathrone.games.jeopardy.dto.GameState;
import de.quantumquest.triviathrone.games.jeopardy.dto.JeopardyStateMessage;
import de.quantumquest.triviathrone.games.jeopardy.dto.JeopardyStateMessage.QuestionTileMessage;
import de.quantumquest.triviathrone.games.jeopardy.dto.JeopardyStateMessage.RoundBoardMessage;
import de.quantumquest.triviathrone.games.jeopardy.definition.Category;
import de.quantumquest.triviathrone.games.jeopardy.dto.MediaType;
import de.quantumquest.triviathrone.games.jeopardy.definition.QuestionDefinition;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

public class JeopardySession implements IGameSession {
    private final GameQuestionBoard board;
    private final GameScoreboard scoreboard;
    private final String lobbyCode;

    private final GameBuzzer buzzer;

    private volatile GameState currentState = GameState.IDLE;
    private volatile Runnable stateChangeListener = () -> {
    };

    private String mediaPlaybackToken;
    private String mediaPauseToken;

    public JeopardySession(String lobbyCode, Set<String> players, JeopardyDefinition definition) {
        Objects.requireNonNull(lobbyCode, "lobbyCode");
        Objects.requireNonNull(players, "players");
        this.board = new GameQuestionBoard(definition);
        this.scoreboard = new GameScoreboard(players);
        this.lobbyCode = lobbyCode;
        this.buzzer = new GameBuzzer(this::onBuzzFinalized);
    }

    public synchronized void selectQuestion(Integer round, Integer category, Integer question) {
        if (currentState != GameState.IDLE) {
            throw new IllegalStateException("Cannot select question now");
        }

        board.selectQuestion(round, category, question);
        buzzer.reset();
        mediaPlaybackToken = null;
        mediaPauseToken = null;
        currentState = GameState.QUESTION_SELECTED;
    }

    public synchronized void playMedia() {
        if (currentState != GameState.QUESTION_SELECTED && currentState != GameState.BUZZ_OPEN) return;

        QuestionDefinition question = board.currentQuestion();
        if ((question.mediaType() == MediaType.AUDIO || question.mediaType() == MediaType.VIDEO)
                && question.mediaPath() != null && !question.mediaPath().isBlank()) {
            mediaPlaybackToken = UUID.randomUUID().toString();
            if (currentState == GameState.QUESTION_SELECTED) {
                buzzer.open();
                currentState = GameState.BUZZ_OPEN;
            }
        }
    }

    public synchronized void selectRound(Integer round) {
        if (currentState != GameState.IDLE) {
            throw new IllegalStateException("Cannot change rounds while a question is active");
        }
        board.selectRound(round);
    }

    public synchronized void openBuzz() {
        if (currentState != GameState.QUESTION_SELECTED) return;
        buzzer.open();
        currentState = GameState.BUZZ_OPEN;
    }

    public synchronized void registerBuzz(String username) {
        Objects.requireNonNull(username, "username");
        if (currentState != GameState.BUZZ_OPEN && currentState != GameState.BUZZ_COLLECTING) return;
        GameBuzzer.Registration registration = buzzer.register(username);
        if (registration == GameBuzzer.Registration.FIRST) {
            currentState = GameState.BUZZ_COLLECTING;
            mediaPauseToken = UUID.randomUUID().toString();
        }
    }

    public synchronized void closeBuzz() {
        if (currentState != GameState.BUZZ_OPEN && currentState != GameState.BUZZ_COLLECTING) return;
        currentState = buzzer.closeBuzz() == null ? GameState.QUESTION_SELECTED : GameState.BUZZ_LOCKED;
    }

    public synchronized void markCorrect() {
        String buzzWinner = buzzer.winner();
        if (currentState != GameState.BUZZ_LOCKED || buzzWinner == null) return;

        scoreboard.addPoints(buzzWinner, board.currentQuestion().points());
        currentState = GameState.ANSWER_REVEAL;
    }

    public synchronized void markIncorrect() {
        String buzzWinner = buzzer.winner();
        if (currentState != GameState.BUZZ_LOCKED || buzzWinner == null) return;

        scoreboard.addPoints(buzzWinner, -(board.currentQuestion().points() / 2));
        buzzer.markIncorrect();
        currentState = GameState.QUESTION_SELECTED;
    }

    public synchronized void resetToIdle() {
        buzzer.reset();
        mediaPlaybackToken = null;
        mediaPauseToken = null;
        currentState = GameState.IDLE;
    }

    private void onBuzzFinalized(String winner) {
        synchronized (this) {
            if (currentState != GameState.BUZZ_COLLECTING) return;
            currentState = winner == null ? GameState.QUESTION_SELECTED : GameState.BUZZ_LOCKED;
        }
        stateChangeListener.run();
    }

    public synchronized void setStateChangeListener(Runnable listener) {
        stateChangeListener = listener == null ? () -> {
        } : listener;
    }

    public synchronized GameQuestionMessage getCurrentQuestionMessage() {
        if (currentState == GameState.IDLE) return null;

        return createGameQuestionMessage(board.currentCategory(), board.currentQuestion());
    }

    public synchronized boolean isQuestionUsed(int round, int category, int question) {
        return board.isQuestionUsed(round, category, question);
    }

    public synchronized boolean hasUnusedQuestions() {
        return board.hasUnusedQuestions();
    }

    public synchronized Set<String> getBuzzedPlayers() {
        return buzzer.getBuzzedPlayers();
    }

    public synchronized void close() {
        buzzer.close();
    }

    @Override
    public synchronized GameActionResult handleAction(
            GameActionMessage action,
            String username,
            boolean isHost
    ) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(username, "username");

        if ("BUZZ".equals(action.action())) {
            if (isHost) {
                return GameActionResult.rejected();
            }
            registerBuzz(username);
            return GameActionResult.allow();
        }
        if (!isHost) {
            return GameActionResult.rejected();
        }

        switch (action.action()) {
            case "SELECT_ROUND" -> selectRound(requiredIndex(action, "roundIdx"));
            case "SELECT_QUESTION" -> selectQuestion(
                    requiredIndex(action, "roundIdx"),
                    requiredIndex(action, "categoryIdx"),
                    requiredIndex(action, "questionIdx")
            );
            case "PLAY_MEDIA" -> playMedia();
            case "OPEN_BUZZ" -> openBuzz();
            case "CLOSE_BUZZ" -> closeBuzz();
            case "MARK_CORRECT" -> markCorrect();
            case "MARK_INCORRECT" -> markIncorrect();
            case "RESET" -> {
                if (!hasUnusedQuestions()) {
                    return GameActionResult.endGame();
                }
                resetToIdle();
            }
            default -> {
                return GameActionResult.rejected();
            }
        }
        return GameActionResult.allow();
    }

    @Override
    public synchronized IGameMessage stateMessage() {
        JeopardyDefinition definition = getDefinition();
        List<RoundBoardMessage> rounds = IntStream.range(0, definition.rounds().size())
                .mapToObj(roundIdx -> {
                    var round = definition.rounds().get(roundIdx);
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
                                                isQuestionUsed(roundIdx, categoryIdx, questionIdx)
                                        );
                                    })
                                    .toList())
                            .toList();
                    return new RoundBoardMessage(round.name(), categories, questions);
                })
                .toList();
        int roundIdx = getCurrentRoundIdx();
        String roundName = roundIdx >= 0 && roundIdx < definition.rounds().size()
                ? definition.rounds().get(roundIdx).name()
                : null;

        return new JeopardyStateMessage(
                lobbyCode,
                "jeopardy",
                getCurrentState(),
                getBuzzWinner(),
                getScores(),
                getCurrentRoundIdx(),
                getCurrentCategoryIdx(),
                getCurrentQuestionIdx(),
                definition.title(),
                rounds,
                roundName,
                getCurrentQuestionMessage(),
                getBuzzedPlayers(),
                getMediaPlaybackToken(),
                getMediaPauseToken()
        );
    }

    @Override
    public synchronized IGameMessage endedMessage(String hostUsername) {
        List<JeopardyEndedMessage.LeaderboardEntry> leaderboard = getScores().entrySet().stream()
                .filter(entry -> !entry.getKey().equals(hostUsername))
                .sorted(Comparator.comparingInt((Map.Entry<String, Integer> entry) -> entry.getValue())
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> new JeopardyEndedMessage.LeaderboardEntry(entry.getKey(), entry.getValue()))
                .toList();
        return new JeopardyEndedMessage(lobbyCode, "jeopardy", true, leaderboard);
    }

    private static int requiredIndex(GameActionMessage action, String name) {
        Object value = action.parameters().get(name);
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("Missing integer action parameter: " + name);
    }

    private GameQuestionMessage createGameQuestionMessage(Category category, QuestionDefinition question) {
        String mediaUrl = question.mediaPath() == null
                ? null
                : "/api/game/" + lobbyCode + "/media?path="
                + URLEncoder.encode(question.mediaPath(), StandardCharsets.UTF_8);

        return new GameQuestionMessage(
                category.name(),
                question.prompt(),
                question.choices(),
                currentState == GameState.ANSWER_REVEAL ? question.correctChoiceIndex() : null,
                question.mediaType(),
                mediaUrl,
                question.points()
        );
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public String getBuzzWinner() {
        return buzzer.winner();
    }

    public Map<String, Integer> getScores() {
        return scoreboard.snapshot();
    }

    public JeopardyDefinition getDefinition() {
        return board.definition();
    }

    public synchronized int getCurrentRoundIdx() {
        return board.currentRoundIdx();
    }

    public synchronized int getCurrentCategoryIdx() {
        return board.currentCategoryIdx();
    }

    public synchronized int getCurrentQuestionIdx() {
        return board.currentQuestionIdx();
    }

    public synchronized String getMediaPlaybackToken() {
        return mediaPlaybackToken;
    }

    public synchronized String getMediaPauseToken() {
        return mediaPauseToken;
    }
}
