package quantumquest.triviathrone.utils;

import java.util.Objects;

/**
 * String validation utilities.
 *
 * @author TiJ
 * @since 2.0.0
 */
public final class StringUtils {
    /**
     * Prevents instantiation of this utility class.
     */
    private StringUtils() {
    }

    /**
     * Requires a non-null, non-blank string.
     *
     * @param value value to validate
     * @param name  logical field name for error reporting
     * @return the validated value
     * @throws NullPointerException     if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is blank
     */
    public static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return value;
    }
}
