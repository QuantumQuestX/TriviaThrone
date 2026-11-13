package de.quantumquest.triviathrone.api.game;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Objects;

public record GameCreationRequest(JsonNode data, Map<String, GameCreationAsset> assets) {
    public GameCreationRequest(JsonNode data) {
        this(data, Map.of());
    }

    public GameCreationRequest {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(assets, "assets");

        if (!data.isObject()) {
            throw new IllegalArgumentException("Game creation data must be a JSON object.");
        }

        data = data.deepCopy();
        assets = Map.copyOf(assets);
    }

    @Override
    public JsonNode data() {
        return data.deepCopy();
    }
}
