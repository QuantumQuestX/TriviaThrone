package quantumquest.triviathrone.api.game;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public record GameCreatorDescriptor(
        String title,
        String description,
        JsonNode editorSchema
) {
    public GameCreatorDescriptor {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(editorSchema, "editorSchema");

        if (!editorSchema.isObject()) {
            throw new IllegalArgumentException("The editor schema must be a JSON object.");
        }

        editorSchema = editorSchema.deepCopy();
    }

    @Override
    public JsonNode editorSchema() {
        return editorSchema.deepCopy();
    }
}
