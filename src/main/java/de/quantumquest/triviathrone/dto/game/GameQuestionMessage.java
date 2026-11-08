package de.quantumquest.triviathrone.dto.game;

import de.quantumquest.triviathrone.game.definition.MediaType;

import java.util.List;

public record GameQuestionMessage(
        String category,
        String prompt,
        List<String> choices,
        Integer correctChoiceIndex,
        MediaType mediaType,
        String mediaUrl,
        int points
) {
    public GameQuestionMessage {
        choices = choices == null ? List.of() : List.copyOf(choices);
    }
}
