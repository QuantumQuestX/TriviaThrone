package de.quantumquest.triviathrone.api.game;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public record GameCreationRequest(JsonNode data) {
    public GameCreationRequest {
        Objects.requireNonNull(data, "data");

        if (!data.isObject()) {
            throw new IllegalArgumentException("Game creation data must be a JSON object.");
        }

        data = data.deepCopy();
    }

    @Override
    public JsonNode data() {
        return data.deepCopy();
    }
}
