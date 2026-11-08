package de.quantumquest.triviathrone.game;

import de.quantumquest.triviathrone.dto.game.GameQuestionMessage;
import de.quantumquest.triviathrone.game.definition.Category;
import de.quantumquest.triviathrone.game.definition.MediaType;
import de.quantumquest.triviathrone.game.definition.QuestionDefinition;
import de.quantumquest.triviathrone.server.Lobby;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class GameSession {

    public enum State {
        IDLE,               // waiting for host to select question
        QUESTION_SELECTED,  // question is visible; waiting for the host to open the buzzer
        QUESTION_OPEN,      // clients see question, waiting for buzzer
        BUZZ_OPEN,          // clients can buzz in
        BUZZ_COLLECTING,    // collect all buzzes
        BUZZ_LOCKED,        // first buzz locked
        ANSWER_REVEAL       // showing correct answer and updating scores
    }

    private final GameQuestionBoard board;
    private final GameScoreboard scoreboard;
    private final String lobbyCode;

    private final GameBuzzer buzzer;

    private volatile State currentState = State.IDLE;
    private volatile Runnable stateChangeListener = () -> {
    };

    private String mediaPlaybackToken;
    private String mediaPauseToken;

    public GameSession(Lobby lobby, GameDefinition definition) {
        Objects.requireNonNull(lobby, "lobby");
        this.board = new GameQuestionBoard(definition);
        this.scoreboard = new GameScoreboard(lobby.listUsernames());
        this.lobbyCode = lobby.getLobbyCode();
        this.buzzer = new GameBuzzer(this::onBuzzFinalized);
    }

    public synchronized void selectQuestion(Integer round, Integer category, Integer question) {
        if (currentState != State.IDLE) {
            throw new IllegalStateException("Cannot select question now");
        }

        board.selectQuestion(round, category, question);
        buzzer.reset();
        mediaPlaybackToken = null;
        mediaPauseToken = null;
        currentState = State.QUESTION_SELECTED;
    }

    public synchronized void playMedia() {
        if (currentState != State.QUESTION_SELECTED && currentState != State.BUZZ_OPEN) return;

        QuestionDefinition question = board.currentQuestion();
        if ((question.mediaType() == MediaType.AUDIO || question.mediaType() == MediaType.VIDEO)
                && question.mediaPath() != null && !question.mediaPath().isBlank()) {
            mediaPlaybackToken = UUID.randomUUID().toString();
            if (currentState == State.QUESTION_SELECTED) {
                buzzer.open();
                currentState = State.BUZZ_OPEN;
            }
        }
    }

    public synchronized void selectRound(Integer round) {
        if (currentState != State.IDLE) {
            throw new IllegalStateException("Cannot change rounds while a question is active");
        }
        board.selectRound(round);
    }

    public synchronized void openBuzz() {
        if (currentState != State.QUESTION_SELECTED) return;
        buzzer.open();
        currentState = State.BUZZ_OPEN;
    }

    public synchronized void registerBuzz(String username) {
        Objects.requireNonNull(username, "username");
        if (currentState != State.BUZZ_OPEN && currentState != State.BUZZ_COLLECTING) return;
        GameBuzzer.Registration registration = buzzer.register(username);
        if (registration == GameBuzzer.Registration.FIRST) {
            currentState = State.BUZZ_COLLECTING;
            mediaPauseToken = UUID.randomUUID().toString();
        }
    }

    public synchronized void closeBuzz() {
        if (currentState != State.BUZZ_OPEN && currentState != State.BUZZ_COLLECTING) return;
        currentState = buzzer.closeBuzz() == null ? State.QUESTION_SELECTED : State.BUZZ_LOCKED;
    }

    public synchronized void markCorrect() {
        String buzzWinner = buzzer.winner();
        if (currentState != State.BUZZ_LOCKED || buzzWinner == null) return;

        scoreboard.addPoints(buzzWinner, board.currentQuestion().points());
        currentState = State.ANSWER_REVEAL;
    }

    public synchronized void markIncorrect() {
        String buzzWinner = buzzer.winner();
        if (currentState != State.BUZZ_LOCKED || buzzWinner == null) return;

        scoreboard.addPoints(buzzWinner, -(board.currentQuestion().points() / 2));
        buzzer.markIncorrect();
        currentState = State.QUESTION_SELECTED;
    }

    public synchronized void resetToIdle() {
        buzzer.reset();
        mediaPlaybackToken = null;
        mediaPauseToken = null;
        currentState = State.IDLE;
    }

    private void onBuzzFinalized(String winner) {
        synchronized (this) {
            if (currentState != State.BUZZ_COLLECTING) return;
            currentState = winner == null ? State.QUESTION_SELECTED : State.BUZZ_LOCKED;
        }
        stateChangeListener.run();
    }

    public synchronized void setStateChangeListener(Runnable listener) {
        stateChangeListener = listener == null ? () -> {
        } : listener;
    }

    public synchronized GameQuestionMessage getCurrentQuestionMessage() {
        if (currentState == State.IDLE) return null;

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

    private GameQuestionMessage createGameQuestionMessage(Category category, QuestionDefinition question) {
        String mediaUrl = question.mediaPath() == null
                ? null
                : "/api/game/" + lobbyCode + "/media?path="
                + URLEncoder.encode(question.mediaPath(), StandardCharsets.UTF_8);

        return new GameQuestionMessage(
                category.name(),
                question.prompt(),
                question.choices(),
                currentState == State.ANSWER_REVEAL ? question.correctChoiceIndex() : null,
                question.mediaType(),
                mediaUrl,
                question.points()
        );
    }

    public State getCurrentState() {
        return currentState;
    }

    public String getBuzzWinner() {
        return buzzer.winner();
    }

    public Map<String, Integer> getScores() {
        return scoreboard.snapshot();
    }

    public GameDefinition getDefinition() {
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
