package de.quantumquest.triviathrone.game;

import de.quantumquest.triviathrone.game.definition.Category;
import de.quantumquest.triviathrone.game.definition.QuestionDefinition;
import de.quantumquest.triviathrone.game.definition.Round;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class GameQuestionBoard {
    private final GameDefinition definition;
    private final Set<String> usedQuestions = ConcurrentHashMap.newKeySet();
    private int currentRoundIdx;
    private int currentCategoryIdx;
    private int currentQuestionIdx;

    GameQuestionBoard(GameDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    void selectQuestion(Integer round, Integer category, Integer question) {
        Objects.requireNonNull(round, "round");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(question, "question");

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

        currentRoundIdx = round;
        currentCategoryIdx = category;
        currentQuestionIdx = question;
        usedQuestions.add(key);
    }

    void selectRound(Integer round) {
        Objects.requireNonNull(round, "round");
        if (round < 0 || round >= definition.rounds().size()) {
            throw new IllegalArgumentException("Round index is out of range");
        }
        currentRoundIdx = round;
    }

    boolean isQuestionUsed(int round, int category, int question) {
        return usedQuestions.contains(questionKey(round, category, question));
    }

    boolean hasUnusedQuestions() {
        int totalQuestions = definition.rounds().stream()
                .flatMap(round -> round.categories().stream())
                .mapToInt(category -> category.questions().size())
                .sum();
        return usedQuestions.size() < totalQuestions;
    }

    GameDefinition definition() {
        return definition;
    }

    Round currentRound() {
        return definition.rounds().get(currentRoundIdx);
    }

    Category currentCategory() {
        return currentRound().categories().get(currentCategoryIdx);
    }

    QuestionDefinition currentQuestion() {
        return currentCategory().questions().get(currentQuestionIdx);
    }

    int currentRoundIdx() {
        return currentRoundIdx;
    }

    int currentCategoryIdx() {
        return currentCategoryIdx;
    }

    int currentQuestionIdx() {
        return currentQuestionIdx;
    }

    private static String questionKey(int round, int category, int question) {
        return round + ":" + category + ":" + question;
    }
}
