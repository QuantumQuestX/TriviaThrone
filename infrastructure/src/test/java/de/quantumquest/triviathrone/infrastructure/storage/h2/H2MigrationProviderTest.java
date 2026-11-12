package de.quantumquest.triviathrone.infrastructure.storage.h2;

import de.quantumquest.triviathrone.api.storage.migration.IMigration;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class H2MigrationProviderTest {
    @Test
    void loadsMigrationIndexFromClasspath() {
        assertTrue(new ClassPathResource("storage/h2/migrations/index.xml").exists());

        List<IMigration> migrations = new H2MigrationProvider().getMigrations();

        assertEquals(1, migrations.size());
        assertEquals(1, migrations.get(0).version());
        assertEquals("p001__create-account-tables.sql", migrations.get(0).source());
    }
}
