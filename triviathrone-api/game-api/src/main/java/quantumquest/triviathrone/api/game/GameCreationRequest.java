package quantumquest.triviathrone.api.game;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Objects;

/**
 * Supplies validated game-creation data and uploaded assets to a creator.
 *
 * <p>The JSON data is defensively copied on construction and access. The asset map is an immutable
 * snapshot, although referenced asset paths remain caller-owned.</p>
 *
 * @param data   non-null JSON object containing creation data
 * @param assets non-null assets keyed by logical filename
 * @author TiJ
 * @since 2.0.0
 */
public record GameCreationRequest(JsonNode data, Map<String, GameCreationAsset> assets) {
    /**
     * Creates a request without assets.
     *
     * @param data non-null JSON object containing creation data
     * @throws NullPointerException     if {@code data} is null
     * @throws IllegalArgumentException if {@code data} is not a JSON object
     */
    public GameCreationRequest(JsonNode data) {
        this(data, Map.of());
    }

    /**
     * Validates and snapshots the request data.
     *
     * @throws NullPointerException     if {@code data} or {@code assets} is null
     * @throws IllegalArgumentException if {@code data} is not a JSON object
     */
    public GameCreationRequest {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(assets, "assets");

        if (!data.isObject()) {
            throw new IllegalArgumentException("Game creation data must be a JSON object.");
        }

        data = data.deepCopy();
        assets = Map.copyOf(assets);
    }

    /**
     * Returns an independent copy of the creation data.
     *
     * @return a non-null mutable copy that is not backed by this request
     */
    @Override
    public JsonNode data() {
        return data.deepCopy();
    }
}
