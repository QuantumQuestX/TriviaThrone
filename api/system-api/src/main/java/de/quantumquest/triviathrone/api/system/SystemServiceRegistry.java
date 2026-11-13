package de.quantumquest.triviathrone.api.system;

import java.util.Optional;

/** Registry for typed system services. Each service interface has at most one provider. */
public interface SystemServiceRegistry {
    <T> void register(Class<T> serviceType, T service);

    <T> Optional<T> find(Class<T> serviceType);
}
