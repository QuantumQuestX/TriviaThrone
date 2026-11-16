package quantumquest.triviathrone.api.shared;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for parsing and comparing semantic API versions.
 *
 * @author TiJ
 * @since 2.1.0
 */
public final class ApiVersion {
    /**
     * Pattern accepted for semantic API versions, including optional qualifiers.
     */
    private static final Pattern VERSION_PATTERN =
            Pattern.compile("^(\\d+)\\.\\d+\\.\\d+(?:[-+][0-9A-Za-z.-]+)?$");

    /**
     * Prevents instantiation of this utility class.
     */
    private ApiVersion() {
    }

    /**
     * Parses a semantic version.
     *
     * @param value version text
     * @return parsed version
     */
    public static Version parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("API version is required.");
        }

        Matcher matcher = VERSION_PATTERN.matcher(value);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid API version: " + value);
        }

        return new Version(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
        );
    }

    /**
     * Compares two semantic versions.
     *
     * @param left first version
     * @param right second version
     * @return comparison result
     */
    public static int compare(String left, String right) {
        return parse(left).compareTo(parse(right));
    }

    /**
     * Tests an inclusive minimum and exclusive maximum range.
     *
     * @param value           version to test
     * @param minimum         inclusive minimum
     * @param maximumExclusive exclusive maximum
     * @return whether the value is within the range
     */
    public static boolean isInRange(String value, String minimum, String maximumExclusive) {
        if (compare(minimum, maximumExclusive) >= 0) {
            throw new IllegalArgumentException("Version minimum must be below maximum.");
        }
        return compare(value, minimum) >= 0 && compare(value, maximumExclusive) < 0;
    }
}
