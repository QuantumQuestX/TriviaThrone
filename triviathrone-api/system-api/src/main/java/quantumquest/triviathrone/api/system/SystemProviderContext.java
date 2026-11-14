package quantumquest.triviathrone.api.system;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable provider-scoped configuration and writable data directory without server-framework types.
 *
 * @param dataDirectory normalized absolute writable provider data directory
 * @param configuration immutable provider configuration map
 * @author TiJ
 * @since 2.0.0
 */
public record SystemProviderContext(Path dataDirectory, Map<String, String> configuration) {
    /**
     * Normalizes the data directory and defensively copies configuration.
     */
    public SystemProviderContext {
        dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory").toAbsolutePath().normalize();
        configuration = Map.copyOf(Objects.requireNonNull(configuration, "configuration"));
    }

    /**
     * Looks up a configuration property.
     *
     * @param name property name, never {@code null}
     * @return value when configured
     */
    public Optional<String> property(String name) {
        return Optional.ofNullable(configuration.get(Objects.requireNonNull(name, "name")));
    }
}
