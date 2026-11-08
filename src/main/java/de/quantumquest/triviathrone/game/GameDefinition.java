package de.quantumquest.triviathrone.game;

import de.quantumquest.triviathrone.game.definition.Round;

import java.util.List;
import java.util.Objects;

public record GameDefinition(String title, String description, List<Round> rounds) {
    public GameDefinition {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        rounds = List.copyOf(Objects.requireNonNull(rounds, "rounds"));
    }
}
