package quantumquest.triviathrone.api.game;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * Describes a game creator and the JSON schema used by its editor.
 *
 * <p>The schema is defensively copied on construction and access.</p>
 *
 * @param title        non-null creator title
 * @param description  non-null creator description
 * @param editorSchema non-null JSON object defining editor fields
 * @author TiJ
 * @since 2.0.0
 */
public record GameCreatorDescriptor(
        String title,
        String description,
        JsonNode editorSchema
) {
    /**
     * Validates and snapshots the editor schema.
     *
     * @throws NullPointerException     if any component is null
     * @throws IllegalArgumentException if {@code editorSchema} is not a JSON object
     */
    public GameCreatorDescriptor {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(editorSchema, "editorSchema");

        if (!editorSchema.isObject()) {
            throw new IllegalArgumentException("The editor schema must be a JSON object.");
        }

        editorSchema = editorSchema.deepCopy();
    }

    /**
     * Returns an independent copy of the editor schema.
     *
     * @return a non-null mutable copy that is not backed by this descriptor
     */
    @Override
    public JsonNode editorSchema() {
        return editorSchema.deepCopy();
    }
}
