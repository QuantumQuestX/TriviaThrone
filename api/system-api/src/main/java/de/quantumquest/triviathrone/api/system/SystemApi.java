package de.quantumquest.triviathrone.api.system;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SystemApi {
    public static final String CURRENT_VERSION = "2.0.0";
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.\\d+\\.\\d+(?:[-+][0-9A-Za-z.-]+)?$");

    private SystemApi() {
    }

    public static boolean isCompatible(String apiVersion) {
        return majorVersion(apiVersion) == majorVersion(CURRENT_VERSION);
    }

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
