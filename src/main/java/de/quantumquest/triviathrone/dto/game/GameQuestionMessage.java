package de.quantumquest.triviathrone.dto.game;

import de.quantumquest.triviathrone.game.definition.MediaType;

import java.util.List;

public record GameQuestionMessage(String category, String prompt, List<String> choices, MediaType mediaType, String mediaUrl, int points) {}
