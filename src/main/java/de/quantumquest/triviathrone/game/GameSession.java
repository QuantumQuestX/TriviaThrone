package de.quantumquest.triviathrone.game;

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
        IDLE,
        QUESTION_SELECTED,
        BUZZ_OPEN,
        BUZZ_COLLECTING,
        BUZZ_LOCKED,
        ANSWER_REVEAL
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

        scores.computeIfPresent(buzzWinner, (_, score) -> score - (currentQuestionIdx / 2));
        currentState = State.ANSWER_REVEAL;
    }

    public synchronized void resetToIdle() {
        buzzWinner = null;
        buzzTimes.clear();
        currentState = State.IDLE;
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
