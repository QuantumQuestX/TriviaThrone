package quantumquest.triviathrone.api.game;

import quantumquest.triviathrone.api.shared.ApiVersion;
import quantumquest.triviathrone.api.shared.Version;

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
    public static final String CURRENT_VERSION = "2.3.1";
    /** API version 2.0.0. */
    public static final Version VERSION_2_0_0 = new Version(2, 0, 0);
    /** API version 2.1.0. */
    public static final Version VERSION_2_1_0 = new Version(2, 1, 0);
    /** API version 2.1.1. */
    public static final Version VERSION_2_1_1 = new Version(2, 1, 1);
    /** API version 2.2.0. */
    public static final Version VERSION_2_2_0 = new Version(2, 2, 0);
    /** API version 2.2.1. */
    public static final Version VERSION_2_2_1 = new Version(2, 2, 1);
    /** API version 2.3.0. */
    public static final Version VERSION_2_3_0 = new Version(2, 3, 0);
    /** API version 2.3.1. */
    public static final Version VERSION_2_3_1 = new Version(2, 3, 1);
    /**
     * Parsed current game API version for feature-dependent provider behavior.
     *
     * <p>Providers can compare this value with the {@code VERSION_*} constants when selecting
     * behavior for a specific API release.</p>
     */
    public static final Version CURRENT_API_VERSION = VERSION_2_3_1;

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
     * Determines whether the current API satisfies a game mod's compatibility declaration.
     *
     * <p>{@code apiVersion} is the inclusive minimum supported version. When no maximum is
     * declared, compatibility follows the API major-version rule.</p>
     *
     * @param apiVersion       inclusive minimum API version supported by the mod
     * @param maximumExclusive optional exclusive maximum API version
     * @return whether the current API is compatible
     */
    public static boolean isCompatible(String apiVersion, String maximumExclusive) {
        if (maximumExclusive == null) {
            return isCompatible(apiVersion);
        }
        return ApiVersion.isInRange(CURRENT_VERSION, apiVersion, maximumExclusive);
    }

    /**
     * Determines compatibility using the legacy explicit range form.
     *
     * <p>This overload remains available for source and binary compatibility with clients that
     * use an independently supplied minimum. New game-mod manifests should use {@link
     * #isCompatible(String, String)}, where {@code apiVersion} is the minimum.</p>
     *
     * @param apiVersion       API version declared by the provider
     * @param minimumVersion   explicit inclusive minimum API version
     * @param maximumExclusive explicit exclusive maximum API version
     * @return whether the current API is compatible
     * @deprecated use {@link #isCompatible(String, String)} for game-mod compatibility checks
     */
    @Deprecated
    public static boolean isCompatible(
            String apiVersion,
            String minimumVersion,
            String maximumExclusive
    ) {
        if (minimumVersion == null) {
            return isCompatible(apiVersion, maximumExclusive);
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
