package de.quantumquest.triviathrone.game.definition;

import java.util.Collections;
import java.util.List;

public record Category(String name, List<QuestionDefinition> questions) {
    @Override
    public List<QuestionDefinition> questions() {
        return Collections.unmodifiableList(questions);
    }
}
