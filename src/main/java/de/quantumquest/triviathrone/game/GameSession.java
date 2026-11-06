package de.quantumquest.triviathrone.game;

import de.quantumquest.triviathrone.dto.game.GameQuestionMessage;
import de.quantumquest.triviathrone.game.definition.Category;
import de.quantumquest.triviathrone.game.definition.MediaType;
import de.quantumquest.triviathrone.game.definition.QuestionDefinition;
import de.quantumquest.triviathrone.game.definition.Round;
import de.quantumquest.triviathrone.server.Lobby;

import java.util.HashSet;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    private static final long BUZZ_THRESHOLD_MS = 120;

    private final GameDefinition definition;
    private final String lobbyCode;

    private final Map<String, Integer> scores = new ConcurrentHashMap<>();
    private final Map<String, Long> buzzTimes = new ConcurrentHashMap<>();
    private final Set<String> usedQuestions = ConcurrentHashMap.newKeySet();
    private final Set<String> attemptedPlayers = ConcurrentHashMap.newKeySet();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "game-buzzer");
        thread.setDaemon(true);
        return thread;
    });

    private volatile State currentState = State.IDLE;
    private volatile String buzzWinner;
    private volatile Runnable stateChangeListener = () -> {
    };
    private ScheduledFuture<?> pendingBuzz;

    private volatile int currentRoundIdx;
    private volatile int currentCategoryIdx;
    private volatile int currentQuestionIdx;
    private volatile int currentQuestionPoints;
    private String mediaPlaybackToken;
    private String mediaPauseToken;

    public GameSession(Lobby lobby, GameDefinition definition) {
        this.definition = definition;
        this.lobbyCode = lobby.getLobbyCode();
        lobby.listUsernames().forEach(username -> scores.put(username, 0));
    }

    public synchronized void selectQuestion(Integer round, Integer category, Integer question) {
        if (round == null || category == null || question == null) {
            throw new IllegalArgumentException("No round, category, or question index given");
        }

        if (currentState != State.IDLE) {
            throw new IllegalStateException("Cannot select question now");
        }

        if (round < 0 || round >= definition.rounds().size()) {
            throw new IllegalArgumentException("Round index is out of range");
        }
        Round roundDefinition = definition.rounds().get(round);
        if (category < 0 || category >= roundDefinition.categories().size()) {
            throw new IllegalArgumentException("Category index is out of range");
        }
        Category categoryDefinition = roundDefinition.categories().get(category);
        if (question < 0 || question >= categoryDefinition.questions().size()) {
            throw new IllegalArgumentException("Question index is out of range");
        }

        String key = questionKey(round, category, question);

        if (usedQuestions.contains(key)) {
            throw new IllegalStateException("Question already used");
        }

        QuestionDefinition q = categoryDefinition.questions().get(question);

        currentRoundIdx = round;
        currentCategoryIdx = category;
        currentQuestionIdx = question;
        currentQuestionPoints = q.points();
        mediaPlaybackToken = null;
        mediaPauseToken = null;
        attemptedPlayers.clear();

        usedQuestions.add(key);

        currentState = State.QUESTION_SELECTED;
    }

    public synchronized void playMedia() {
        if (currentState != State.QUESTION_SELECTED && currentState != State.BUZZ_OPEN) return;

        QuestionDefinition question = definition.rounds().get(currentRoundIdx)
                .categories().get(currentCategoryIdx)
                .questions().get(currentQuestionIdx);
        if ((question.mediaType() == MediaType.AUDIO || question.mediaType() == MediaType.VIDEO)
                && question.mediaPath() != null && !question.mediaPath().isBlank()) {
            mediaPlaybackToken = UUID.randomUUID().toString();
            if (currentState == State.QUESTION_SELECTED) {
                buzzWinner = null;
                buzzTimes.clear();
                currentState = State.BUZZ_OPEN;
            }
        }
    }

    public synchronized void selectRound(Integer round) {
        if (currentState != State.IDLE) {
            throw new IllegalStateException("Cannot change rounds while a question is active");
        }
        if (round == null || round < 0 || round >= definition.rounds().size()) {
            throw new IllegalArgumentException("Round index is out of range");
        }
        currentRoundIdx = round;
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
            if (attemptedPlayers.contains(username)) return;

            long now = System.nanoTime();
            buzzTimes.putIfAbsent(username, now);

            if (currentState == State.BUZZ_OPEN) {
                currentState = State.BUZZ_COLLECTING;
                mediaPauseToken = UUID.randomUUID().toString();

                pendingBuzz = scheduler.schedule(
                        this::finalizeBuzz,
                        BUZZ_THRESHOLD_MS,
                        TimeUnit.MILLISECONDS
                );
            }
        }
    }

    public void closeBuzz() {
        synchronized (this) {
            if (currentState == State.BUZZ_OPEN) {
                currentState = State.QUESTION_SELECTED;
            } else if (currentState == State.BUZZ_COLLECTING) {
                finalizeBuzzLocked();
            }
        }
    }

    private void finalizeBuzz() {
        synchronized (this) {
            if (currentState != State.BUZZ_COLLECTING) return;
            finalizeBuzzLocked();
        }
        stateChangeListener.run();
    }

    private void finalizeBuzzLocked() {
        pendingBuzz = null;
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
        if (currentState != State.BUZZ_LOCKED || buzzWinner == null) return;

        scores.computeIfPresent(buzzWinner, (_, score) -> score + currentQuestionPoints);
        currentState = State.ANSWER_REVEAL;
    }

    public synchronized void markIncorrect() {
        if (currentState != State.BUZZ_LOCKED || buzzWinner == null) return;

        scores.computeIfPresent(buzzWinner, (_, score) -> score - (currentQuestionPoints / 2));
        attemptedPlayers.add(buzzWinner);
        buzzWinner = null;
        buzzTimes.clear();
        currentState = State.QUESTION_SELECTED;
    }

    public synchronized void resetToIdle() {
        if (pendingBuzz != null) {
            pendingBuzz.cancel(false);
            pendingBuzz = null;
        }
        buzzWinner = null;
        buzzTimes.clear();
        attemptedPlayers.clear();
        mediaPlaybackToken = null;
        mediaPauseToken = null;
        currentState = State.IDLE;
    }

    public synchronized void setStateChangeListener(Runnable listener) {
        stateChangeListener = listener == null ? () -> {
        } : listener;
    }

    public synchronized GameQuestionMessage getCurrentQuestionMessage() {
        if (currentState == State.IDLE) return null;

        Round round = definition.rounds().get(currentRoundIdx);
        Category category = round.categories().get(currentCategoryIdx);
        return createGameQuestionMessage(
                category,
                category.questions().get(currentQuestionIdx)
        );
    }

    public synchronized boolean isQuestionUsed(int round, int category, int question) {
        return usedQuestions.contains(questionKey(round, category, question));
    }

    public synchronized boolean hasUnusedQuestions() {
        int totalQuestions = definition.rounds().stream()
                .flatMap(round -> round.categories().stream())
                .mapToInt(category -> category.questions().size())
                .sum();
        return usedQuestions.size() < totalQuestions;
    }

    public synchronized Set<String> getBuzzedPlayers() {
        Set<String> buzzed = new HashSet<>(attemptedPlayers);
        buzzed.addAll(buzzTimes.keySet());
        return Set.copyOf(buzzed);
    }

    public synchronized void close() {
        if (pendingBuzz != null) pendingBuzz.cancel(false);
        scheduler.shutdownNow();
    }

    private String questionKey(int round, int category, int question) {
        return round + ":" + category + ":" + question;
    }

    private GameQuestionMessage createGameQuestionMessage(Category c, QuestionDefinition q) {
        String mediaUrl = q.mediaPath() == null
                ? null
                : "/api/game/" + lobbyCode + "/media?path="
                + URLEncoder.encode(q.mediaPath(), StandardCharsets.UTF_8);

        return new GameQuestionMessage(
                c.name(),
                q.prompt(),
                null,
                q.mediaType(),
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
        return Map.copyOf(scores);
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

    public synchronized String getMediaPlaybackToken() {
        return mediaPlaybackToken;
    }

    public synchronized String getMediaPauseToken() {
        return mediaPauseToken;
    }
}
