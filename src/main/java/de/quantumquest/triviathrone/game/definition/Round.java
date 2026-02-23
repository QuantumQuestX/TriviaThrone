package de.quantumquest.triviathrone.game.definition;

import java.util.Collections;
import java.util.List;

public record Round(String name, List<Category> categories) {
    @Override
    public List<Category> categories() {
        return Collections.unmodifiableList(categories);
    }
}
