package de.quantumquest.triviathrone.game.definition;

import java.util.List;

public record QuestionDefinition(
        int points,
        String prompt,
        String mediaPath,
        MediaType mediaType,
        List<String> choices,
        Integer correctChoiceIndex
) {
    public QuestionDefinition {
        choices = choices == null ? List.of() : List.copyOf(choices);
        if (choices.isEmpty() != (correctChoiceIndex == null)
                || correctChoiceIndex != null
                && (correctChoiceIndex < 0 || correctChoiceIndex >= choices.size())) {
            throw new IllegalArgumentException("Correct choice index must refer to an available choice");
        }
    }

    public QuestionDefinition(int points, String prompt, String mediaPath, MediaType mediaType) {
        this(points, prompt, mediaPath, mediaType, List.of(), null);
    }
}
