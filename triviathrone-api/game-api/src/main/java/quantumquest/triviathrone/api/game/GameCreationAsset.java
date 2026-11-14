package quantumquest.triviathrone.api.game;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Identifies a source asset supplied to a game creator.
 *
 * @param filename non-blank logical filename supplied by the caller
 * @param path     non-null path to the asset; ownership remains with the caller
 * @author TiJ
 * @since 2.0.0
 */
public record GameCreationAsset(String filename, Path path) {
    /**
     * Validates the supplied asset identity.
     *
     * @throws IllegalArgumentException if {@code filename} is null or blank
     * @throws NullPointerException     if {@code path} is null
     */
    public GameCreationAsset {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Asset filename is required.");
        }
        Objects.requireNonNull(path, "path");
    }
}
