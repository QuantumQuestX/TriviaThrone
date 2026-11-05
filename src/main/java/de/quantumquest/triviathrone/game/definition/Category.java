package de.quantumquest.triviathrone.game.definition;

import java.util.List;

public record Category(String name, List<QuestionDefinition> questions) {
    public Category {
        questions = List.copyOf(questions);
    }
}
