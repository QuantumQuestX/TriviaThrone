package de.quantumquest.triviathrone.api.system;

/** A system-mod lifecycle entry point declared through Java ServiceLoader. */
public interface ISystemProvider extends AutoCloseable {
    SystemProviderDescriptor descriptor();

    /** Called once during server startup; registered services become read-only after initialization. */
    void initialize(SystemProviderContext context, SystemServiceRegistry services) throws Exception;

    /** Called in reverse initialization order during shutdown or after a startup failure. */
    @Override
    default void close() throws Exception {
    }
}
