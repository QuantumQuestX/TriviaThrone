package de.quantumquest.triviathrone.auth;

import de.quantumquest.triviathrone.dto.UserFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserConfigLoader {
    private final Map<String, UserAccount> users = new ConcurrentHashMap<>();
    private final UserXmlMigrationImporter xmlMigrationImporter;
    private final IUserAccountRepository accountDatabase;
    private final String initialPassword;
    private final PasswordEncoder passwordEncoder;

    public UserConfigLoader(
            AuthProperties props,
            PasswordEncoder passwordEncoder,
            UserXmlMigrationImporter xmlMigrationImporter,
            IUserAccountRepository accountDatabase
    ) {
        AuthProperties properties = Objects.requireNonNull(props, "props");
        this.xmlMigrationImporter = Objects.requireNonNull(xmlMigrationImporter, "xmlMigrationImporter");
        this.accountDatabase = Objects.requireNonNull(accountDatabase, "accountDatabase");
        this.initialPassword = properties.initialPassword();
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");

        load();
    }

    private void load() {
        List<UserAccount> accounts = accountDatabase.findAll();
        boolean importXml = accounts.isEmpty();
        if (importXml) {
            UserFile migrationData = xmlMigrationImporter.load();
            if (migrationData.getUsers() == null || migrationData.getUsers().isEmpty()) {
                throw new IllegalStateException("User XML migration file is empty or malformed");
            }
            accounts = migrationData.getUsers();
        }

        for (UserAccount user : accounts) {
            if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
                throw new IllegalStateException("User data contains an invalid user");
            }
            if (user.getPasswordHash() == null) {
                if (initialPassword == null || initialPassword.isBlank()) {
                    throw new IllegalStateException(
                            "initialPassword is missing for " + xmlMigrationImporter.fileName()
                    );
                }

                user.setPasswordHash(passwordEncoder.encode(initialPassword));
            }
        }

        if (importXml) accountDatabase.insertAll(accounts);
        users.clear();
        accounts.forEach(user -> users.put(user.getUsername(), user));
    }

    public UserAccount findByUsername(String username) {
        return users.get(Objects.requireNonNull(username, "username"));
    }

    public synchronized void updatePassword(String username, String newHash) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(newHash, "newHash");
        UserAccount user = users.get(username);

        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        accountDatabase.updatePassword(username, newHash);
        user.setPasswordHash(newHash);
    }
}
