package de.quantumquest.triviathrone.game;

import de.quantumquest.triviathrone.dto.game.GameQuestionMessage;
import de.quantumquest.triviathrone.dto.game.GameStateMessage;
import de.quantumquest.triviathrone.game.definition.Category;
import de.quantumquest.triviathrone.game.definition.MediaType;
import de.quantumquest.triviathrone.game.definition.QuestionDefinition;
import de.quantumquest.triviathrone.server.Lobby;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameSession {

    public enum State {
        IDLE,               // waiting for host to select question
        QUESTION_SELECTED,  // question selected, not visible to clients yet
        QUESTION_OPEN,      // clients see question, waiting for buzzer
        BUZZ_OPEN,          // clients can buzz in
        BUZZ_COLLECTING,    // collect all buzzes
        BUZZ_LOCKED,        // first buzz locked
        ANSWER_REVEAL       // showing correct answer and updating scores
    }

    private static final long BUZZ_THRESHOLD_MS = 120;

    private final Lobby lobby;
    private final GameDefinition definition;

    private final Map<String, Integer> scores = new ConcurrentHashMap<>();
    private final Map<String, Long> buzzTimes = new ConcurrentHashMap<>();
    private final Set<String> usedQuestions = ConcurrentHashMap.newKeySet();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private volatile State currentState = State.IDLE;
    private volatile String buzzWinner;

    private volatile int currentRoundIdx;
    private volatile int currentCategoryIdx;
    private volatile int currentQuestionIdx;
    private volatile int currentQuestionPoints;

    public GameSession(Lobby lobby, GameDefinition definition) {
        this.lobby = lobby;
        this.definition = definition;
        lobby.listUsernames().forEach(username -> scores.put(username, 0));
    }

    public synchronized void selectQuestion(Integer round, Integer category, Integer question) {
        if (round == null || category == null || question == null) {
            throw new IllegalArgumentException("No round, category, or question index given");
        }

        if (currentState != State.IDLE) {
            throw new IllegalStateException("Cannot select question now");
        }

        String key = round + ":" + category + ":" + question;

        if (usedQuestions.contains(key)) {
            throw new IllegalStateException("Question already used");
        }

        QuestionDefinition q = definition.rounds()
                .get(round)
                .categories().get(category)
                .questions().get(question);

        currentRoundIdx = round;
        currentCategoryIdx = category;
        currentQuestionIdx = question;
        currentQuestionPoints = q.points();

        usedQuestions.add(key);

        currentState = State.QUESTION_SELECTED;
    }

    public void openBuzz() {
        synchronized (this) {
            if (currentState != State.QUESTION_SELECTED) return;

            buzzWinner = null;
            buzzTimes.clear();
            currentState = State.BUZZ_OPEN;
        }
    }

    public void registerBuzz(String username) {
        synchronized (this) {
            if (currentState != State.BUZZ_OPEN &&
                currentState != State.BUZZ_COLLECTING) return;

            long now = System.nanoTime();
            buzzTimes.putIfAbsent(username, now);

            if (currentState == State.BUZZ_OPEN) {
                currentState = State.BUZZ_COLLECTING;

                scheduler.schedule(
                        this::finalizeBuzz,
                        BUZZ_THRESHOLD_MS,
                        TimeUnit.MILLISECONDS
                );
            }
        }
    }

    private synchronized void finalizeBuzz() {
        if (buzzTimes.isEmpty()) {
            currentState = State.QUESTION_SELECTED;
            return;
        }

        buzzWinner = buzzTimes.entrySet()
                .stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        currentState = State.BUZZ_LOCKED;
    }

    public synchronized void markCorrect() {
        if (buzzWinner == null) return;

        scores.computeIfPresent(buzzWinner, (_, score) -> score + currentQuestionPoints);
        currentState = State.ANSWER_REVEAL;
    }

    public synchronized void markIncorrect() {
        if (buzzWinner == null) return;

        scores.computeIfPresent(buzzWinner, (_, score) -> score - (currentQuestionPoints / 2));
        currentState = State.ANSWER_REVEAL;
    }

    public synchronized void resetToIdle() {
        buzzWinner = null;
        buzzTimes.clear();
        currentState = State.IDLE;
    }

    private GameQuestionMessage createGameQuestionMessage(Category c, QuestionDefinition q) {
        String mediaUrl = q.mediaPath();
        MediaType mediaType = MediaType.UNDEFINED;

        if (mediaUrl != null && !mediaUrl.isBlank()) {
            if (mediaUrl.endsWith(".mp4")) {
                mediaType = MediaType.VIDEO;
            } else if (mediaUrl.endsWith(".mp3") || mediaUrl.endsWith(".wav")) {
                mediaType = MediaType.AUDIO;
            } else {
                mediaType = MediaType.IMAGE;
            }
        }

        return new GameQuestionMessage(
                c.name(),
                q.prompt(),
                null,
                mediaType,
                mediaUrl,
                q.points()
        );
    }

    public State getCurrentState() {
        return currentState;
    }

    public String getBuzzWinner() {
        return buzzWinner;
    }

    public Map<String, Integer> getScores() {
        return scores;
    }

    public GameDefinition getDefinition() {
        return definition;
    }

    public int getCurrentRoundIdx() {
        return currentRoundIdx;
    }

    public int getCurrentCategoryIdx() {
        return currentCategoryIdx;
    }

    public int getCurrentQuestionIdx() {
        return currentQuestionIdx;
    }
}
