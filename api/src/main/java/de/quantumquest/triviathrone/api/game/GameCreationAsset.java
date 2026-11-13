package de.quantumquest.triviathrone.api.game;

import java.nio.file.Path;
import java.util.Objects;

public record GameCreationAsset(String filename, Path path) {
    public GameCreationAsset {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Asset filename is required.");
        }
        Objects.requireNonNull(path, "path");
    }
}
