package de.quantumquest.triviathrone.api.system;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Provider-scoped configuration and writable data directory, with no server-framework types. */
public record SystemProviderContext(Path dataDirectory, Map<String, String> configuration) {
    public SystemProviderContext {
        dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory").toAbsolutePath().normalize();
        configuration = Map.copyOf(Objects.requireNonNull(configuration, "configuration"));
    }

    public Optional<String> property(String name) {
        return Optional.ofNullable(configuration.get(Objects.requireNonNull(name, "name")));
    }
}
