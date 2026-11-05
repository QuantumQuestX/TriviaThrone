package de.quantumquest.triviathrone.game.definition;

import java.util.List;

public record Round(String name, List<Category> categories) {
    public Round {
        categories = List.copyOf(categories);
    }
}
