package quantumquest.triviathrone.api.storage.migration;

public interface IMigration {
    int version();

    String description();

    String source();

    void apply(IMigrationContext context);
}
