package quantumquest.triviathrone.api.system;

import java.util.Objects;

public record SystemProviderDescriptor(String id, String version, String apiVersion) {
    public SystemProviderDescriptor {
        requireText(id, "id");
        if (!id.matches("[a-z0-9][a-z0-9._-]*")) {
            throw new IllegalArgumentException("System provider id must be lowercase and stable: " + id);
        }
        requireText(version, "version");
        requireText(apiVersion, "apiVersion");
    }

    private static void requireText(String value, String name) {
        if (Objects.requireNonNull(value, name).isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
    }
}
