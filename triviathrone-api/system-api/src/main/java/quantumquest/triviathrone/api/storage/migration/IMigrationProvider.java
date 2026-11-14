package quantumquest.triviathrone.api.storage.migration;

import java.util.List;

/**
 * Supplies the complete ordered set of migrations for a storage implementation.
 *
 * @author TiJ
 * @since 2.0.0
 */
public interface IMigrationProvider {
    /**
     * Returns migrations that the runner compares with persisted migration state.
     */
    List<IMigration> getMigrations();
}
