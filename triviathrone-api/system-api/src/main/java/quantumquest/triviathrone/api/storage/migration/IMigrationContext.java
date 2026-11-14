package quantumquest.triviathrone.api.storage.migration;

/**
 * Execution context supplied to a database migration.
 *
 * @author TiJ
 * @since 2.0.0
 */
public interface IMigrationContext {
    /**
     * Executes the supplied migration source against the target storage.
     */
    void execute(String source);
}
