package de.quantumquest.triviathrone.game;

import de.quantumquest.triviathrone.game.definition.Round;

import java.util.List;

public record GameDefinition(String title, String description, List<Round> rounds) {
    public GameDefinition {
        rounds = List.copyOf(rounds);
    }
}
