package quantumquest.triviathrone.api.storage.migration;

/**
 * An ordered, one-time storage schema migration.
 *
 * @author TiJ
 * @since 2.0.0
 */
public interface IMigration {
    /**
     * Returns the unique, monotonically ordered migration version.
     */
    int version();

    /**
     * Returns a human-readable description of this migration.
     */
    String description();

    /**
     * Returns the source executed to apply this migration.
     */
    String source();

    /**
     * Applies this migration using the supplied execution context.
     */
    void apply(IMigrationContext context);
}
