package de.quantumquest.triviathrone.api.storage.migration;

public interface IMigrationContext {
    void execute(String source);
}
