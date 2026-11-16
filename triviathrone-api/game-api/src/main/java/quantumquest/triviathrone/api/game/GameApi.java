package quantumquest.triviathrone.api.game;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities and version information for the game-provider API.
 *
 * @author TiJ
 * @since 2.0.0
 */
public final class GameApi {
    /**
     * Current semantic version of this API. Compatibility is determined by major version.
     */
    public static final String CURRENT_VERSION = "2.0.0";
    /**
     * Pattern that extracts the major component of an accepted semantic version.
     */
    private static final Pattern VERSION_PATTERN =
            Pattern.compile("^(\\d+)\\.\\d+\\.\\d+(?:[-+][0-9A-Za-z.-]+)?$");

    /**
     * Prevents instantiation of this utility class.
     */
    private GameApi() {
    }

    /**
     * Determines whether an API version has the current major version.
     *
     * @param apiVersion semantic API version to check
     * @return whether the version is compatible
     * @throws IllegalArgumentException if the version is absent or invalid
     */
    public static boolean isCompatible(String apiVersion) {
        return isCompatibleMajor(majorVersion(apiVersion));
    }

    /**
     * Determines whether the current API satisfies an optional compatibility range.
     *
     * @param apiVersion         API version the provider targets
     * @param minimumVersion     inclusive minimum, or {@code null} for major-version compatibility
     * @param maximumExclusive   exclusive maximum, or {@code null} for major-version compatibility
     * @return whether the current API is compatible
     */
    public static boolean isCompatible(
            String apiVersion,
            String minimumVersion,
            String maximumExclusive
    ) {
        if (minimumVersion == null && maximumExclusive == null) {
            return isCompatible(apiVersion);
        }
        if (minimumVersion == null || maximumExclusive == null) {
            throw new IllegalArgumentException("API compatibility requires both bounds.");
        }
        int declared = compareVersions(CURRENT_VERSION, minimumVersion);
        int maximum = compareVersions(CURRENT_VERSION, maximumExclusive);
        if (compareVersions(minimumVersion, maximumExclusive) >= 0) {
            throw new IllegalArgumentException("API compatibility minimum must be below maximum.");
        }
        return declared >= 0 && maximum < 0 && isCompatible(apiVersion);
    }

    /**
     * Determines whether an API major version is compatible with this runtime.
     *
     * @param apiMajorVersion provider API major version
     * @return whether the major versions match
     */
    public static boolean isCompatibleMajor(int apiMajorVersion) {
        return apiMajorVersion == majorVersion(CURRENT_VERSION);
    }

    /**
     * Parses the major component of a semantic version.
     *
     * @param version semantic API version to parse
     * @return parsed major version
     * @throws IllegalArgumentException if the version is absent or invalid
     */
    private static int majorVersion(String version) {
        if (version == null) {
            throw new IllegalArgumentException("API version is required.");
        }
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid API version: " + version);
        }
        return Integer.parseInt(matcher.group(1));
    }

    /**
     * Compares semantic versions by major, minor, and patch components.
     *
     * @param left  first version
     * @param right second version
     * @return comparison result
     */
    private static int compareVersions(String left, String right) {
        int[] leftParts = versionParts(left);
        int[] rightParts = versionParts(right);
        for (int i = 0; i < leftParts.length; i++) {
            int comparison = Integer.compare(leftParts[i], rightParts[i]);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    /**
     * Parses the numeric semantic version components.
     *
     * @param version semantic version
     * @return major, minor, and patch components
     */
    private static int[] versionParts(String version) {
        if (version == null) {
            throw new IllegalArgumentException("API version is required.");
        }
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid API version: " + version);
        }
        String[] parts = version.split("[.+-]", 4);
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
    }
}
