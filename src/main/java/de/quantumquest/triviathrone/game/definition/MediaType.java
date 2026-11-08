package de.quantumquest.triviathrone.game.definition;

import org.apache.tika.Tika;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public enum MediaType {
    AUDIO,
    VIDEO,
    IMAGE,
    UNDEFINED;

    public static MediaType detect(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        if (!Files.isRegularFile(file)) {
            return UNDEFINED;
        }

        String contentType;
        try (InputStream content = Files.newInputStream(file)) {
            contentType = new Tika().detect(content);
        }

        if (contentType.startsWith("image/")) {
            return IMAGE;
        }
        if (contentType.startsWith("audio/")) {
            return AUDIO;
        }
        if (contentType.startsWith("video/")) {
            return VIDEO;
        }
        return UNDEFINED;
    }
}
