package quantumquest.triviathrone.api.system;

import quantumquest.triviathrone.api.system.events.ISystemEvent;

/**
 * A system-mod lifecycle entry point discovered through {@link java.util.ServiceLoader}.
 * Implementations must expose a public no-argument constructor, use an API-compatible
 * descriptor, and make concurrent event handling safe.
 *
 * @author TiJ
 * @since 2.0.0
 */
public interface ISystemProvider extends AutoCloseable {
    /**
     * Returns the stable descriptor used to identify and validate this provider.
     */
    SystemProviderDescriptor descriptor();

    /**
     * Initializes this provider once during startup; registrations become read-only afterward.
     *
     * @param context  provider-scoped immutable configuration and data location
     * @param services registry to which initialization may register services
     * @throws Exception if initialization cannot complete
     */
    void initialize(SystemProviderContext context, SystemServiceRegistry services) throws Exception;

    /**
     * Handles a server lifecycle event synchronously; invocations may be concurrent.
     *
     * @param event immutable event payload
     */
    default void onEvent(ISystemEvent event) {
    }

    /**
     * Releases resources in reverse initialization order during shutdown or startup rollback.
     *
     * @throws Exception if cleanup fails
     */
    @Override
    default void close() throws Exception {
    }
}
