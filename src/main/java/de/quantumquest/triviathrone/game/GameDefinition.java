package de.quantumquest.triviathrone.game;

import de.quantumquest.triviathrone.game.definition.Round;

import java.util.Collections;
import java.util.List;

public record GameDefinition(String title, String description, List<Round> rounds) {
    @Override
    public List<Round> rounds() {
        return Collections.unmodifiableList(rounds);
    }
}
