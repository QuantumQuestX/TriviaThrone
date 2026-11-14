package quantumquest.triviathrone.api.storage.migration;

import java.util.List;

/**
 * Persists the migration versions already applied to a storage target.
 *
 * @author TiJ
 * @since 2.0.0
 */
public interface IMigrationStateStore {
    /**
     * Returns every version recorded as successfully applied.
     */
    List<Integer> getAppliedVersions();

    /**
     * Records a migration after its application succeeds.
     */
    void recordApplied(IMigration migration);
}
