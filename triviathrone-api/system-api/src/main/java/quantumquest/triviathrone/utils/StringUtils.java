package quantumquest.triviathrone.utils;

import java.util.Objects;

public final class StringUtils {
    private StringUtils() {
    }

    public static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return value;
    }
}
