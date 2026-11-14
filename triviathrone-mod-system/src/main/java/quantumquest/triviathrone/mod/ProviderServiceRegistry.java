package quantumquest.triviathrone.mod;

import quantumquest.triviathrone.api.system.SystemServiceRegistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Mutable, load-phase-only registry for services supplied by system providers.
 *
 * @author TiJ
 * @since 2.0.0
 */
final class ProviderServiceRegistry implements SystemServiceRegistry {
    /**
     * Maps each service interface to its registered implementation.
     */
    private final Map<Class<?>, Object> services = new LinkedHashMap<>();
    /**
     * Maps each service interface to the identifier of its registering provider.
     */
    private final Map<Class<?>, String> serviceOwners = new LinkedHashMap<>();
    /**
     * Indicates whether provider initialization may still register services.
     */
    private boolean acceptingRegistrations = true;
    /**
     * Identifies the provider currently being initialized, when any.
     */
    private String registeringProviderId;

    /**
     * Marks a provider as the current registration owner.
     *
     * @param providerId non-null provider identifier
     * @throws NullPointerException if {@code providerId} is {@code null}
     */
    synchronized void beginProviderInitialization(String providerId) {
        registeringProviderId = Objects.requireNonNull(providerId, "providerId");
    }

    /**
     * Clears the current registration owner after provider initialization.
     */
    synchronized void endProviderInitialization() {
        registeringProviderId = null;
    }

    /**
     * Permanently closes the registry to additional service registrations.
     */
    synchronized void finishLoading() {
        acceptingRegistrations = false;
    }

    /**
     * Prevents registrations while shutdown is in progress.
     */
    synchronized void stopRegistration() {
        acceptingRegistrations = false;
    }

    /**
     * Removes all registered services and ownership information.
     */
    synchronized void clear() {
        services.clear();
        serviceOwners.clear();
    }

    /**
     * Registers an implementation under its service interface during provider initialization.
     *
     * @param serviceType interface implemented by {@code service}
     * @param service     service implementation
     * @param <T>         service interface type
     * @throws NullPointerException     if either argument is {@code null}
     * @throws IllegalArgumentException if {@code serviceType} is not an interface
     * @throws IllegalStateException    if loading has finished or the interface is already registered
     */
    @Override
    public synchronized <T> void register(Class<T> serviceType, T service) {
        if (!acceptingRegistrations) {
            throw new IllegalStateException("System services can only be registered during provider initialization.");
        }

        Class<T> contract = Objects.requireNonNull(serviceType, "serviceType");
        if (!contract.isInterface()) {
            throw new IllegalArgumentException("System services must be registered by interface type.");
        }

        Object implementation = contract.cast(Objects.requireNonNull(service, "service"));

        String existingOwner = serviceOwners.get(contract);
        if (services.putIfAbsent(contract, implementation) != null) {
            throw new IllegalStateException(
                "System service " + contract.getName() + " is already registered by " + existingOwner
            );
        }

        serviceOwners.put(contract, registeringProviderId == null ? "unknown" : registeringProviderId);
    }

    /**
     * Finds the service registered under an interface.
     *
     * @param serviceType interface to look up
     * @param <T>         service interface type
     * @return the registered service, or an empty optional
     * @throws NullPointerException if {@code serviceType} is {@code null}
     */
    @Override
    public synchronized <T> Optional<T> find(Class<T> serviceType) {
        Class<T> contract = Objects.requireNonNull(serviceType, "serviceType");

        return Optional.ofNullable(services.get(contract)).map(contract::cast);
    }
}
