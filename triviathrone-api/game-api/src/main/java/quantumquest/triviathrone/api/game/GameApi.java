package quantumquest.triviathrone.api.game;

import quantumquest.triviathrone.api.shared.ApiVersion;

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
    public static final String CURRENT_VERSION = "2.2.0";

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
        return isCompatibleMajor(ApiVersion.parse(apiVersion).major());
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

        return ApiVersion.isInRange(CURRENT_VERSION, minimumVersion, maximumExclusive)
                && isCompatible(apiVersion);
    }

    /**
     * Determines whether an API major version is compatible with this runtime.
     *
     * @param apiMajorVersion provider API major version
     * @return whether the major versions match
     */
    public static boolean isCompatibleMajor(int apiMajorVersion) {
        return apiMajorVersion == ApiVersion.parse(CURRENT_VERSION).major();
    }
}
