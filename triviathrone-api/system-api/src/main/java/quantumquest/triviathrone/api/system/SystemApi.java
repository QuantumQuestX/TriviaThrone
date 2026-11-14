package quantumquest.triviathrone.api.system;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public static final String CURRENT_VERSION = "2.0.0";
    /**
     * Pattern that extracts the major component of an accepted semantic version.
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.\\d+\\.\\d+(?:[-+][0-9A-Za-z.-]+)?$");

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
        return isCompatibleMajor(majorVersion(apiVersion));
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
     * @param version semantic version to parse
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
}
