package quantumquest.triviathrone.api.storage.migration;

import java.util.List;

public interface IMigrationStateStore {
    List<Integer> getAppliedVersions();

    void recordApplied(IMigration migration);
}
