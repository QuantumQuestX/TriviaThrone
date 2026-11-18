package quantumquest.triviathrone.api.system;

import quantumquest.triviathrone.api.shared.ApiVersion;

/**
 * Utilities and version information for the system-provider API.
 *
 * @author TiJ
 * @since 2.0.0
 */
public final class SystemApi {
    /**
     * Current semantic version of this API. Compatibility is determined by major version.
     */
    public static final String CURRENT_VERSION = "2.2.0";

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
        return isCompatibleMajor(ApiVersion.parse(apiVersion).major());
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
