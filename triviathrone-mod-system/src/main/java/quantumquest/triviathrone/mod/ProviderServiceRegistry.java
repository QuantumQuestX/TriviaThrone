package quantumquest.triviathrone.mod;

import quantumquest.triviathrone.api.system.SystemServiceRegistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class ProviderServiceRegistry implements SystemServiceRegistry {
    private final Map<Class<?>, Object> services = new LinkedHashMap<>();
    private final Map<Class<?>, String> serviceOwners = new LinkedHashMap<>();
    private boolean acceptingRegistrations = true;
    private String registeringProviderId;

    synchronized void beginProviderInitialization(String providerId) {
        registeringProviderId = Objects.requireNonNull(providerId, "providerId");
    }

    synchronized void endProviderInitialization() {
        registeringProviderId = null;
    }

    synchronized void finishLoading() {
        acceptingRegistrations = false;
    }

    synchronized void stopRegistration() {
        acceptingRegistrations = false;
    }

    synchronized void clear() {
        services.clear();
        serviceOwners.clear();
    }

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

    @Override
    public synchronized <T> Optional<T> find(Class<T> serviceType) {
        Class<T> contract = Objects.requireNonNull(serviceType, "serviceType");

        return Optional.ofNullable(services.get(contract)).map(contract::cast);
    }
}
