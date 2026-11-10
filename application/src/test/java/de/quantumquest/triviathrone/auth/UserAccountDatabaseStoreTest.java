package de.quantumquest.triviathrone.auth;

import de.quantumquest.triviathrone.infrastructure.storage.h2.H2UserAccountRepository;
import de.quantumquest.triviathrone.infrastructure.storage.h2.H2MigrationRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAccountDatabaseStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void importsXmlOnceAndPersistsAccountsAndPasswordChangesInH2() throws Exception {
        Path xmlFile = tempDir.resolve("users.xml");
        Path databaseFile = tempDir.resolve("user-accounts");
        Files.writeString(xmlFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE users SYSTEM "users.dtd">
                <users xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:noNamespaceSchemaLocation="users.xsd">
                    <user>
                        <username>player</username>
                        <displayName>Trivia Player</displayName>
                        <roles><role>HOST</role></roles>
                    </user>
                </users>
                """);
        AuthProperties properties = properties(xmlFile);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        DataSource dataSource = dataSource(databaseFile);

        UserConfigLoader firstStart = newLoader(properties, dataSource, passwordEncoder);

        UserAccount importedUser = firstStart.findByUsername("player");
        assertNotNull(importedUser);
        assertEquals("Trivia Player", importedUser.getDisplayName());
        assertTrue(importedUser.getRoles().contains("HOST"));
        assertUuidV4WithoutDashes(importedUser.getUserId());
        assertTrue(passwordEncoder.matches("TestPassword#123", importedUser.getPasswordHash()));
        assertTrue(Files.exists(Path.of(databaseFile + ".mv.db")));

        Files.delete(xmlFile);
        String updatedHash = passwordEncoder.encode("UpdatedPassword#456");
        firstStart.updatePassword("player", updatedHash);

        UserConfigLoader restarted = newLoader(properties, dataSource, passwordEncoder);

        UserAccount persistedUser = restarted.findByUsername("player");
        assertNotNull(persistedUser);
        assertEquals("Trivia Player", persistedUser.getDisplayName());
        assertTrue(persistedUser.getRoles().contains("HOST"));
        assertEquals(importedUser.getUserId(), persistedUser.getUserId());
        assertTrue(passwordEncoder.matches("UpdatedPassword#456", persistedUser.getPasswordHash()));
    }

    @Test
    void rejectsInvalidXmlDuringInitialImport() throws Exception {
        Path xmlFile = tempDir.resolve("users.xml");
        Files.writeString(xmlFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE users SYSTEM "users.dtd">
                <users xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:noNamespaceSchemaLocation="users.xsd">
                    <user><displayName>Missing username</displayName></user>
                </users>
                """);
        AuthProperties properties = properties(xmlFile);
        DataSource dataSource = dataSource(tempDir.resolve("invalid-users"));

        assertThrows(IllegalStateException.class, () -> newLoader(
                properties,
                dataSource,
                new BCryptPasswordEncoder()
        ));
    }

    @Test
    void doesNotUseYamlAsAnImportSource() throws Exception {
        Path xmlFile = tempDir.resolve("users.xml");
        Path yamlFile = tempDir.resolve("users.yml");
        Files.writeString(yamlFile, "users: [{username: player, passwordHash: hash}]");
        AuthProperties properties = properties(xmlFile);

        assertThrows(IllegalStateException.class, () -> newLoader(
                properties,
                dataSource(tempDir.resolve("yaml-users")),
                new BCryptPasswordEncoder()
        ));
        assertTrue(Files.exists(yamlFile));
        assertFalse(Files.exists(xmlFile));
    }

    private UserConfigLoader newLoader(
            AuthProperties properties,
            DataSource dataSource,
            BCryptPasswordEncoder passwordEncoder
    ) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        H2UserAccountRepository databaseStore = new H2UserAccountRepository(
                jdbcTemplate,
                transactionManager,
                new H2MigrationRunner(jdbcTemplate, transactionManager)
        );
        return new UserConfigLoader(
                properties,
                passwordEncoder,
                new UserXmlMigrationImporter(properties),
                databaseStore
        );
    }

    private DataSource dataSource(Path databaseFile) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:file:" + databaseFile.toAbsolutePath() + ";DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private AuthProperties properties(Path userMigrationFile) {
        return new AuthProperties(
                userMigrationFile,
                tempDir.resolve("refresh-tokens.xml"),
                "01234567890123456789012345678901",
                900,
                3600,
                7200,
                false,
                "TestPassword#123"
        );
    }

    private void assertUuidV4WithoutDashes(String userId) {
        assertTrue(userId.matches("[0-9a-f]{12}4[0-9a-f]{3}[89ab][0-9a-f]{15}"));
    }
}
