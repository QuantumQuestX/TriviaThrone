package quantumquest.triviathrone.api.system;

import quantumquest.triviathrone.api.shared.ApiVersion;
import quantumquest.triviathrone.api.shared.Version;

/**
 * Utilities and version information for the system-provider API.
 *
 * @author TiJ
 * @since 2.0.0
 */
public final class SystemApi {
    /**
     * Current semantic version of this API. Providers must require this major version
     * and no newer minor or patch version.
     */
    public static final String CURRENT_VERSION = "2.3.0";
    /** System API version 2.0.0. */
    public static final Version VERSION_2_0_0 = new Version(2, 0, 0);
    /** System API version 2.1.0. */
    public static final Version VERSION_2_1_0 = new Version(2, 1, 0);
    /** System API version 2.1.1. */
    public static final Version VERSION_2_1_1 = new Version(2, 1, 1);
    /** System API version 2.2.0. */
    public static final Version VERSION_2_2_0 = new Version(2, 2, 0);
    /** System API version 2.2.1. */
    public static final Version VERSION_2_2_1 = new Version(2, 2, 1);
    /** System API version 2.3.0. */
    public static final Version VERSION_2_3_0 = new Version(2, 3, 0);
    /**
     * Parsed current system API version for feature-dependent provider behavior.
     */
    public static final Version CURRENT_API_VERSION = VERSION_2_3_0;

    /**
     * Prevents instantiation of this utility class.
     */
    private SystemApi() {
    }

    /**
     * Determines whether an API version has the current major version.
     *
     * @param apiVersion semantic API version to check
     * @return whether the version is compatible
     * @throws IllegalArgumentException if the version is absent or invalid
     */
    public static boolean isCompatible(String apiVersion) {
        return isCompatibleMajor(ApiVersion.parse(apiVersion).major())
                && ApiVersion.compare(apiVersion, CURRENT_VERSION) <= 0;
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
