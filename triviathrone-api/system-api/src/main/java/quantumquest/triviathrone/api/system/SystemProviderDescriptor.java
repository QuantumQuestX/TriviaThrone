package quantumquest.triviathrone.api.system;

import java.util.Objects;

/**
 * Immutable identity and compatibility metadata for a discovered system provider.
 *
 * @param id         stable lowercase provider identifier
 * @param version    provider implementation version
 * @param apiVersion system API version required by the provider
 * @author TiJ
 * @since 2.0.0
 */
public record SystemProviderDescriptor(String id, String version, String apiVersion) {
    /**
     * Validates the provider identifier and required version metadata.
     */
    public SystemProviderDescriptor {
        requireText(id, "id");
        if (!id.matches("[a-z0-9][a-z0-9._-]*")) {
            throw new IllegalArgumentException("System provider id must be lowercase and stable: " + id);
        }
        requireText(version, "version");
        requireText(apiVersion, "apiVersion");
    }

    /**
     * Requires a non-null, non-blank metadata value.
     *
     * @param value value to validate
     * @param name  logical field name for error reporting
     * @throws NullPointerException     if the value is {@code null}
     * @throws IllegalArgumentException if the value is blank
     */
    private static void requireText(String value, String name) {
        if (Objects.requireNonNull(value, name).isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
    }
}
