package quantumquest.triviathrone.mod;

import quantumquest.triviathrone.api.system.events.ISystemEvent;
import quantumquest.triviathrone.api.system.ISystemProvider;
import quantumquest.triviathrone.api.system.SystemApi;
import quantumquest.triviathrone.api.system.SystemCompatibility;
import quantumquest.triviathrone.api.system.SystemProviderContext;
import quantumquest.triviathrone.api.system.SystemProviderDescriptor;
import quantumquest.triviathrone.api.system.SystemServiceRegistry;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Stream;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SystemModManager implements SystemServiceRegistry, AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(SystemModManager.class.getName());

    private final List<URLClassLoader> classLoaders = new ArrayList<>();
    private final List<ISystemProvider> providers = new ArrayList<>();
    private final Map<Class<?>, Object> services = new LinkedHashMap<>();
    private final Map<Class<?>, String> serviceOwners = new LinkedHashMap<>();
    private final Set<String> providerIds = new HashSet<>();
    private boolean acceptingRegistrations = true;
    private volatile boolean closed;
    private String registeringProviderId;

    private SystemModManager() {
    }

    public static SystemModManager load(Path modsDirectory, SystemProviderContext context) throws IOException {
        return load(modsDirectory, context, ISystemProvider.class.getClassLoader());
    }

    static SystemModManager load(
            Path modsDirectory,
            SystemProviderContext context,
            ClassLoader parentClassLoader
    ) throws IOException {
        Objects.requireNonNull(modsDirectory, "modsDirectory");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(parentClassLoader, "parentClassLoader");

        SystemModManager manager = new SystemModManager();
        try {
            Files.createDirectories(modsDirectory);
            Files.createDirectories(context.dataDirectory());
            try (Stream<Path> entries = Files.list(modsDirectory)) {
                List<Path> jars = entries
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                        .sorted()
                        .toList();
                for (Path jar : jars) {
                    manager.loadJar(jar, context, parentClassLoader);
                }
            }
            manager.acceptingRegistrations = false;
            return manager;
        } catch (IOException | RuntimeException | ServiceConfigurationError | LinkageError exception) {
            manager.closeAfterFailure(exception);
            throw exception;
        } catch (Exception exception) {
            preserveInterrupt(exception);
            manager.closeAfterFailure(exception);
            throw new IOException("Could not initialize system mods.", exception);
        }
    }

    private void loadJar(Path jar, SystemProviderContext context, ClassLoader parentClassLoader) throws Exception {
        URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, parentClassLoader);
        classLoaders.add(classLoader);

        List<ISystemProvider> discovered = ServiceLoader
                .load(ISystemProvider.class, classLoader)
                .stream()
                .filter(provider -> provider.type().getClassLoader() == classLoader)
                .map(ServiceLoader.Provider::get)
                .toList();
        if (discovered.isEmpty()) {
            throw new IllegalStateException("No system providers found in mod JAR: " + jar);
        }

        for (ISystemProvider provider : discovered) {
            SystemProviderDescriptor descriptor = Objects.requireNonNull(provider.descriptor(), "provider descriptor");
            SystemCompatibility compatibility = provider.getClass().getAnnotation(SystemCompatibility.class);
            if (compatibility != null && !SystemApi.isCompatibleMajor(compatibility.major())) {
                throw new IllegalStateException(
                        "System mod " + descriptor.id() + " requires compatibility level "
                                + compatibility.major() + "; server provides " + SystemApi.CURRENT_VERSION
                );
            }
            if (!SystemApi.isCompatible(descriptor.apiVersion())) {
                throw new IllegalStateException(
                        "System mod " + descriptor.id() + " requires API " + descriptor.apiVersion()
                                + "; server provides " + SystemApi.CURRENT_VERSION
                );
            }
            if (!providerIds.add(descriptor.id())) {
                throw new IllegalStateException("Duplicate system provider id: " + descriptor.id());
            }

            SystemProviderContext providerContext = new SystemProviderContext(
                    context.dataDirectory().resolve(descriptor.id()),
                    providerConfiguration(context.configuration(), descriptor.id())
            );
            Files.createDirectories(providerContext.dataDirectory());
            providers.add(provider);
            registeringProviderId = descriptor.id();
            try {
                provider.initialize(providerContext, this);
            } finally {
                registeringProviderId = null;
            }
        }
    }

    @Override
    public synchronized <T> void register(Class<T> serviceType, T service) {
        if (!acceptingRegistrations || closed) {
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

    public List<SystemProviderDescriptor> providers() {
        return providers.stream().map(ISystemProvider::descriptor).toList();
    }

    public void publishEvent(ISystemEvent event) {
        Objects.requireNonNull(event, "event");
        if (closed) {
            throw new IllegalStateException("System providers are closed.");
        }

        for (ISystemProvider provider : List.copyOf(providers)) {
            try {
                provider.onEvent(event);
            } catch (RuntimeException exception) {
                LOGGER.log(
                        Level.SEVERE,
                        "System provider " + provider.getClass().getName()
                                + " failed while handling " + event.getClass().getSimpleName() + ".",
                        exception
                );
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        acceptingRegistrations = false;
        IOException failure = null;

        for (int i = providers.size() - 1; i >= 0; i--) {
            try {
                providers.get(i).close();
            } catch (Exception exception) {
                preserveInterrupt(exception);
                failure = accumulate(failure, "Could not close system provider.", exception);
            }
        }
        for (int i = classLoaders.size() - 1; i >= 0; i--) {
            try {
                classLoaders.get(i).close();
            } catch (IOException exception) {
                failure = accumulate(failure, "Could not close system mod class loader.", exception);
            }
        }

        providers.clear();
        classLoaders.clear();
        services.clear();
        serviceOwners.clear();
        providerIds.clear();
        if (failure != null) {
            throw failure;
        }
    }

    private void closeAfterFailure(Throwable failure) {
        try {
            close();
        } catch (IOException closeException) {
            failure.addSuppressed(closeException);
        }
    }

    private static Map<String, String> providerConfiguration(Map<String, String> configuration, String providerId) {
        String prefix = "triviathrone.system." + providerId + ".";
        Map<String, String> providerConfiguration = new LinkedHashMap<>();
        configuration.forEach((key, value) -> {
            if (key.startsWith(prefix)) {
                providerConfiguration.put(key.substring(prefix.length()), value);
            }
        });
        return Map.copyOf(providerConfiguration);
    }

    private static IOException accumulate(IOException failure, String message, Exception exception) {
        if (failure == null) {
            return new IOException(message, exception);
        }
        failure.addSuppressed(exception);
        return failure;
    }

    private static void preserveInterrupt(Exception exception) {
        if (exception instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
