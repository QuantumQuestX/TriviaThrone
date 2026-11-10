package de.quantumquest.triviathrone.games.jeopardy.definition;

import java.util.List;
import java.util.Objects;

public record Category(String name, List<QuestionDefinition> questions) {
    public Category {
        Objects.requireNonNull(name, "name");
        questions = List.copyOf(Objects.requireNonNull(questions, "questions"));
    }
}
