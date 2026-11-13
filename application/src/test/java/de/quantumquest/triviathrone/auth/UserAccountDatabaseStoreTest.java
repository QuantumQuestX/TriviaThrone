package de.quantumquest.triviathrone.auth;

import de.quantumquest.triviathrone.infrastructure.storage.h2.H2MigrationRunner;
import de.quantumquest.triviathrone.infrastructure.storage.h2.H2UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAccountDatabaseStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void provisionsDefaultAdminAndPersistsAccountsInH2() {
        Path databaseFile = tempDir.resolve("user-accounts");
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthProperties properties = properties();
        DataSource dataSource = dataSource(databaseFile);

        UserConfigLoader firstStart = newLoader(properties, dataSource, passwordEncoder);

        UserAccount defaultAdmin = firstStart.findByUsername(UserConfigLoader.DEFAULT_ADMIN_USERNAME);
        assertNotNull(defaultAdmin);
        assertTrue(defaultAdmin.isDefaultAdmin());
        assertTrue(defaultAdmin.isEnabled());
        assertTrue(defaultAdmin.getRoles().contains(UserAccount.ROLE_USER));
        assertTrue(defaultAdmin.getRoles().contains(UserAccount.ROLE_ADMIN));
        assertTrue(passwordEncoder.matches("default_password", defaultAdmin.getPasswordHash()));
        assertTrue(Files.exists(Path.of(databaseFile + ".mv.db")));

        UserAccount adminWithoutUserRole = new UserAccount();
        adminWithoutUserRole.setUsername("admin-without-user-role");
        adminWithoutUserRole.setPasswordHash(passwordEncoder.encode("AdminOnly#123"));
        adminWithoutUserRole.setRoles(Set.of(UserAccount.ROLE_ADMIN));
        firstStart.createUser(adminWithoutUserRole);
        firstStart.reconcileDefaultAdmin();
        assertTrue(defaultAdmin.isEnabled());

        UserAccount player = new UserAccount();
        player.setUsername("player");
        player.setDisplayName("Trivia Player");
        player.setPasswordHash(passwordEncoder.encode("TestPassword#123"));
        player.setRoles(Set.of("HOST"));
        firstStart.createUser(player);

        String updatedHash = passwordEncoder.encode("UpdatedPassword#456");
        firstStart.updatePassword("player", updatedHash);

        UserConfigLoader restarted = newLoader(properties, dataSource, passwordEncoder);

        UserAccount persistedPlayer = restarted.findByUsername("player");
        assertNotNull(persistedPlayer);
        assertEquals(player.getUserId(), persistedPlayer.getUserId());
        assertEquals("Trivia Player", persistedPlayer.getDisplayName());
        assertEquals(Set.of("HOST"), persistedPlayer.getRoles());
        assertTrue(passwordEncoder.matches("UpdatedPassword#456", persistedPlayer.getPasswordHash()));
        assertTrue(passwordEncoder.matches(
                "default_password",
                restarted.findByUsername(UserConfigLoader.DEFAULT_ADMIN_USERNAME).getPasswordHash()
        ));
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
        return new UserConfigLoader(properties, passwordEncoder, databaseStore);
    }

    private DataSource dataSource(Path databaseFile) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:file:" + databaseFile.toAbsolutePath() + ";DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private AuthProperties properties() {
        return new AuthProperties(
                tempDir.resolve("refresh-tokens.xml"),
                "01234567890123456789012345678901",
                900,
                3600,
                7200,
                false,
                "",
                ""
        );
    }
}
