package quantumquest.triviathrone.api.storage.migration;

import java.util.List;

public interface IMigrationProvider {
    List<IMigration> getMigrations();
}
