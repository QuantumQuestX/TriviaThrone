package quantumquest.triviathrone.api.system;

import java.util.Optional;

/**
 * Registry for typed system services. Each service interface has at most one provider.
 * Registrations are permitted only during provider initialization.
 *
 * @author TiJ
 * @since 2.0.0
 */
public interface SystemServiceRegistry {
    /**
     * Registers the sole implementation for a service type.
     *
     * @param serviceType service interface token
     * @param service     service implementation
     * @param <T>         service type
     */
    <T> void register(Class<T> serviceType, T service);

    /**
     * Finds the implementation registered for a service type.
     *
     * @param serviceType service interface token
     * @param <T>         service type
     * @return registered service, if present
     */
    <T> Optional<T> find(Class<T> serviceType);
}
