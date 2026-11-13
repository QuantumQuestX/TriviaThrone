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

        assertEquals(4, migrations.size());
        assertEquals(1, migrations.get(0).version());
        assertEquals("p001__create-account-tables.sql", migrations.get(0).source());
        assertEquals(2, migrations.get(1).version());
        assertEquals("p002__enable-user-accounts.sql", migrations.get(1).source());
        assertEquals(3, migrations.get(2).version());
        assertEquals("p003__mark-default-admin.sql", migrations.get(2).source());
        assertEquals(4, migrations.get(3).version());
        assertEquals("p004__seed-default-admin.sql", migrations.get(3).source());
    }
}
