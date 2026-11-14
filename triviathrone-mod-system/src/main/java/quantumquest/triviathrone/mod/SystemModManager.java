package quantumquest.triviathrone.mod;

import quantumquest.triviathrone.api.system.ISystemProvider;
import quantumquest.triviathrone.api.system.SystemApi;
import quantumquest.triviathrone.api.system.SystemCompatibility;
import quantumquest.triviathrone.api.system.SystemProviderContext;
import quantumquest.triviathrone.api.system.SystemProviderDescriptor;
import quantumquest.triviathrone.api.system.SystemServiceRegistry;
import quantumquest.triviathrone.api.system.events.ISystemEvent;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Loads system-provider JARs, coordinates their lifecycle, and dispatches system events.
 *
 * <p>Only providers loaded from each mod's dedicated class loader are accepted, preventing
 * parent-class-path implementations from being mistaken for mod content.</p>
 *
 * @author TiJ
 * @since 2.0.0
 */
public final class SystemModManager implements SystemServiceRegistry, AutoCloseable {
    /**
     * Logger for provider event-handler failures.
     */
    private static final Logger LOGGER = Logger.getLogger(SystemModManager.class.getName());

    /**
     * Class loaders retained for successfully loaded system mod JARs.
     */
    private final List<URLClassLoader> classLoaders = new ArrayList<>();
    /**
     * Initialized providers in load order.
     */
    private final List<ISystemProvider> providers = new ArrayList<>();
    /**
     * Registry shared with providers during initialization.
     */
    private final ProviderServiceRegistry serviceRegistry = new ProviderServiceRegistry();
    /**
     * Provider identifiers claimed during this load.
     */
    private final Set<String> providerIds = new HashSet<>();
    /**
     * Indicates that shutdown has started or completed.
     */
    private volatile boolean closed;

    /**
     * Creates an initially empty manager.
     */
    private SystemModManager() {
    }

    /**
     * Loads system providers from JARs in a directory using the API class loader as their parent.
     *
     * @param modsDirectory directory containing system-mod JARs
     * @param context       shared host context
     * @return an initialized manager
     * @throws IOException           if a directory or JAR cannot be accessed, or initialization fails
     * @throws NullPointerException  if an argument is {@code null}
     * @throws IllegalStateException if provider compatibility or identifiers are invalid
     */
    public static SystemModManager load(Path modsDirectory, SystemProviderContext context) throws IOException {
        return load(modsDirectory, context, ISystemProvider.class.getClassLoader());
    }

    /**
     * Loads system providers using an explicit parent class loader.
     *
     * @param modsDirectory     directory containing system-mod JARs
     * @param context           shared host context
     * @param parentClassLoader parent used to isolate each mod class loader
     * @return an initialized manager
     * @throws IOException           if a directory or JAR cannot be accessed, or initialization fails
     * @throws NullPointerException  if an argument is {@code null}
     * @throws IllegalStateException if provider compatibility or identifiers are invalid
     */
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
            manager.serviceRegistry.finishLoading();
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

    /**
     * Extracts configuration entries scoped to one provider.
     *
     * @param configuration complete host configuration
     * @param providerId    provider identifier
     * @return immutable configuration with the provider prefix removed
     */
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

    /**
     * Adds a close failure to an aggregate exception.
     *
     * @param failure   existing aggregate, or {@code null}
     * @param message   message for a newly created aggregate
     * @param exception close failure to include
     * @return the resulting aggregate
     */
    private static IOException accumulate(IOException failure, String message, Exception exception) {
        if (failure == null) {
            return new IOException(message, exception);
        }
        failure.addSuppressed(exception);
        return failure;
    }

    /**
     * Restores interrupt status when a caught exception represents interruption.
     *
     * @param exception caught exception
     */
    private static void preserveInterrupt(Exception exception) {
        if (exception instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Discovers, validates, and initializes providers from one JAR.
     *
     * @param jar               system-mod JAR
     * @param context           shared host context
     * @param parentClassLoader parent used for the mod class loader
     * @throws Exception if provider discovery, validation, or initialization fails
     */
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
            serviceRegistry.beginProviderInitialization(descriptor.id());
            try {
                provider.initialize(providerContext, this);
            } finally {
                serviceRegistry.endProviderInitialization();
            }
        }
    }

    /**
     * Registers a service on behalf of the provider currently being initialized.
     *
     * @param serviceType service interface
     * @param service     implementation
     * @param <T>         service interface type
     * @throws IllegalStateException if registration is closed or duplicated
     */
    @Override
    public synchronized <T> void register(Class<T> serviceType, T service) {
        serviceRegistry.register(serviceType, service);
    }

    /**
     * Finds a registered system service.
     *
     * @param serviceType service interface
     * @param <T>         service interface type
     * @return the service, if registered
     */
    @Override
    public synchronized <T> Optional<T> find(Class<T> serviceType) {
        return serviceRegistry.find(serviceType);
    }

    /**
     * Returns descriptors for initialized providers.
     *
     * @return provider descriptors in load order
     */
    public List<SystemProviderDescriptor> providers() {
        return providers.stream().map(ISystemProvider::descriptor).toList();
    }

    /**
     * Delivers an event to each current provider, logging but isolating handler failures.
     *
     * @param event event to publish
     * @throws NullPointerException  if {@code event} is {@code null}
     * @throws IllegalStateException if this manager is closed
     */
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

    /**
     * Closes providers and class loaders in reverse load order and clears all state.
     *
     * @throws IOException if one or more resources cannot be closed
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        serviceRegistry.stopRegistration();

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
        serviceRegistry.clear();
        providerIds.clear();

        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Closes partially initialized resources and attaches any close failure to the original failure.
     *
     * @param failure failure that interrupted loading
     */
    private void closeAfterFailure(Throwable failure) {
        try {
            close();
        } catch (IOException closeException) {
            failure.addSuppressed(closeException);
        }
    }
}
