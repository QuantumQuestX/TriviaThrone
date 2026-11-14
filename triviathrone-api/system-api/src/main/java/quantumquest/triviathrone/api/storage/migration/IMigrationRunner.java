package quantumquest.triviathrone.api.storage.migration;

/**
 * Coordinates application of outstanding storage migrations.
 *
 * @author TiJ
 * @since 2.0.0
 */
public interface IMigrationRunner {
    /**
     * Applies each unapplied migration and records it only after successful application.
     */
    void migrate();
}
