package de.quantumquest.triviathrone.infrastructure.storage.h2;

import de.quantumquest.triviathrone.auth.IUserAccountRepository;
import de.quantumquest.triviathrone.auth.UserAccount;
import de.quantumquest.triviathrone.api.storage.migration.IMigrationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Repository
public class H2UserAccountRepository implements IUserAccountRepository {
    private static final String SQL_STATEMENT__FIND_ALL = """
                SELECT account.user_id, account.username, account.display_name, account.password_hash, account_role.role_name
                FROM user_accounts account
                LEFT JOIN user_account_roles account_role ON account_role.username = account.username
                ORDER BY account.username, account_role.role_name
                """;
    private static final String SQL_STATEMENT__INSERT_ALL = "INSERT INTO user_accounts (user_id, username, display_name, password_hash) VALUES (?, ?, ?, ?)";
    private static final String SQL_STATEMENT__INSERT_ALL_ROLES = "INSERT INTO user_account_roles (username, role_name) VALUES (?, ?)";
    private static final String SQL_STATEMENT__UPDATE_PASSWORD = "UPDATE user_accounts SET password_hash = ? WHERE username = ?";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public H2UserAccountRepository(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            IMigrationRunner migrationRunner
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.transactionTemplate = new TransactionTemplate(
                Objects.requireNonNull(transactionManager, "transactionManager")
        );
        Objects.requireNonNull(migrationRunner, "migrationRunner").migrate();
    }

    @Override
    public List<UserAccount> findAll() {
        return jdbcTemplate.query(SQL_STATEMENT__FIND_ALL, this::readAccounts);
    }

    @Override
    public void insertAll(List<UserAccount> accounts) {
        Objects.requireNonNull(accounts, "accounts");
        transactionTemplate.executeWithoutResult(status -> {
            for (UserAccount account : accounts) {
                UserAccount user = Objects.requireNonNull(account, "account");

                jdbcTemplate.update(
                        SQL_STATEMENT__INSERT_ALL,
                        Objects.requireNonNull(user.getUserId(), "userId"),
                        Objects.requireNonNull(user.getUsername(), "username"),
                        user.getDisplayName(),
                        Objects.requireNonNull(user.getPasswordHash(), "passwordHash")
                );

                for (String role : user.getRoles()) {
                    jdbcTemplate.update(
                            SQL_STATEMENT__INSERT_ALL_ROLES,
                            user.getUsername(),
                            role
                    );
                }
            }
        });
    }

    @Override
    public void updatePassword(String username, String passwordHash) {
        int updatedRows = jdbcTemplate.update(
                SQL_STATEMENT__UPDATE_PASSWORD,
                Objects.requireNonNull(passwordHash, "passwordHash"),
                Objects.requireNonNull(username, "username")
        );
        if (updatedRows != 1) {
            throw new IllegalArgumentException("User not found");
        }
    }

    private List<UserAccount> readAccounts(ResultSet resultSet) throws SQLException {
        Map<String, UserAccount> accounts = new LinkedHashMap<>();
        Map<String, Set<String>> rolesByUsername = new HashMap<>();
        while (resultSet.next()) {
            String userId = resultSet.getString("user_id");
            String username = resultSet.getString("username");
            UserAccount account = accounts.get(username);
            if (account == null) {
                UserAccount user = new UserAccount();
                user.setUserId(userId);
                user.setUsername(username);
                user.setDisplayName(resultSet.getString("display_name"));
                user.setPasswordHash(resultSet.getString("password_hash"));
                accounts.put(username, user);
                account = user;
            }
            String role = resultSet.getString("role_name");
            if (role != null) {
                rolesByUsername.computeIfAbsent(account.getUsername(), key -> new HashSet<>()).add(role);
            }
        }
        accounts.forEach((username, account) ->
                account.setRoles(rolesByUsername.getOrDefault(username, Set.of())));
        return new ArrayList<>(accounts.values());
    }
}
