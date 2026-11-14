package quantumquest.triviathrone.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains the in-memory user-account view backed by the configured repository.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Component
public class UserConfigLoader {
    /**
     * Creates a UserConfigLoader.
     *
     * @param props the configuration properties
     * @param passwordEncoder the password encoder
     * @param accountDatabase the account repository
     */
    public static final String DEFAULT_ADMIN_USERNAME = "admin";

    /**
     * Stores an internal helper required by this component.
     */
    private final Map<String, UserAccount> users = new ConcurrentHashMap<>();
    /**
     * Holds the accoun atabase used by this component.
     */
    private final IUserAccountRepository accountDatabase;
    /**
     * Holds the initia dmi sername used by this component.
     */
    private final String initialAdminUsername;
    /**
     * Holds the initia dmi assword used by this component.
     */
    private final String initialAdminPassword;
    /**
     * Holds the password encoder used by this component.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a UserConfigLoader.
     *
     * @param props           the configuration properties
     * @param passwordEncoder the password encoder
     * @param accountDatabase the account repository
     */
    public UserConfigLoader(
        AuthProperties props,
        PasswordEncoder passwordEncoder,
        IUserAccountRepository accountDatabase
    ) {
        AuthProperties properties = Objects.requireNonNull(props, "props");
        this.accountDatabase = Objects.requireNonNull(accountDatabase, "accountDatabase");
        this.initialAdminUsername = properties.initialAdminUsername();
        this.initialAdminPassword = properties.initialAdminPassword();
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");

        load();
    }

    /**
     * Loads and validates configured user accounts.
     */
    private void load() {
        List<UserAccount> accounts = accountDatabase.findAll();
        for (UserAccount user : accounts) {
            if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
                throw new IllegalStateException("User data contains an invalid user");
            }
        }

        users.clear();
        accounts.forEach(user -> users.put(user.getUsername(), user));

        validateDefaultAdminAccount();
        if (initialAdminUsername != null && !initialAdminUsername.isBlank()
            && !DEFAULT_ADMIN_USERNAME.equals(initialAdminUsername)) {
            provisionInitialAdmin(initialAdminUsername, initialAdminPassword);
        }
        reconcileDefaultAdmin();
    }

    /**
     * Finds an account by username.
     *
     * @param username the account username
     * @return the matching account, or {@code null} when absent
     */
    public UserAccount findByUsername(String username) {
        return users.get(Objects.requireNonNull(username, "username"));
    }

    /**
     * Updates a user's stored password hash.
     *
     * @param username the account username
     * @param newHash  the new hash
     */
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

    /**
     * Returns a snapshot of all configured accounts.
     *
     * @return an immutable snapshot of configured accounts
     */
    public synchronized List<UserAccount> findAll() {
        return List.copyOf(users.values());
    }

    /**
     * Creates a validated user account.
     *
     * @param user the user account
     */
    public synchronized void createUser(UserAccount user) {
        UserAccount account = Objects.requireNonNull(user, "user");
        if (users.containsKey(account.getUsername())) {
            throw new IllegalArgumentException("Username is already in use");
        }
        accountDatabase.insert(account);
        users.put(account.getUsername(), account);
    }

    /**
     * Persists and applies an account's roles.
     *
     * @param username the account username
     * @param roles    the requested account roles
     */
    public synchronized void updateRoles(String username, Set<String> roles) {
        UserAccount user = users.get(Objects.requireNonNull(username, "username"));
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        Set<String> updatedRoles = Set.copyOf(Objects.requireNonNull(roles, "roles"));
        accountDatabase.updateRoles(username, updatedRoles);
        user.setRoles(updatedRoles);
    }

    /**
     * Deletes a non-default account.
     *
     * @param username the account username
     */
    public synchronized void deleteUser(String username) {
        String accountName = Objects.requireNonNull(username, "username");
        UserAccount user = users.get(accountName);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        if (user.isDefaultAdmin()) {
            throw new IllegalStateException("The default admin account cannot be deleted");
        }
        accountDatabase.delete(accountName);
        users.remove(accountName);
    }

    /**
     * Persists and applies an account's enabled state.
     *
     * @param username the account username
     * @param enabled  the requested enabled state
     */
    public synchronized void updateEnabled(String username, boolean enabled) {
        UserAccount user = users.get(Objects.requireNonNull(username, "username"));
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        accountDatabase.updateEnabled(username, enabled);
        user.setEnabled(enabled);
    }

    /**
     * Persists the default-administrator flag for an account.
     *
     * @param username     the account username
     * @param defaultAdmin whether the account is the default administrator
     */
    public synchronized void updateDefaultAdmin(String username, boolean defaultAdmin) {
        UserAccount user = users.get(Objects.requireNonNull(username, "username"));
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        accountDatabase.updateDefaultAdmin(username, defaultAdmin);
        user.setDefaultAdmin(defaultAdmin);
    }

    /**
     * Enables the default administrator only while no other active administrator exists.
     */
    public synchronized void reconcileDefaultAdmin() {
        UserAccount defaultAdmin = users.get(DEFAULT_ADMIN_USERNAME);
        if (defaultAdmin == null || !defaultAdmin.isDefaultAdmin()) {
            throw new IllegalStateException("Default admin account is missing");
        }
        boolean anotherAdminExists = users.values().stream()
            .filter(user -> !user.isDefaultAdmin())
            .anyMatch(user -> user.isEnabled()
                && user.getRoles().contains(UserAccount.ROLE_USER)
                && user.getRoles().contains(UserAccount.ROLE_ADMIN));
        boolean shouldBeEnabled = !anotherAdminExists;
        if (defaultAdmin.isEnabled() != shouldBeEnabled) {
            updateEnabled(DEFAULT_ADMIN_USERNAME, shouldBeEnabled);
        }
    }

    /**
     * Creates or upgrades the optional initial administrator account.
     */
    private void provisionInitialAdmin(String username, String password) {
        UserAccount account = users.get(username);
        if (account == null) {
            PasswordUtils.validate(password);
            account = new UserAccount();
            account.setUsername(username);
            account.setDisplayName(username);
            account.setPasswordHash(passwordEncoder.encode(password));
            account.setRoles(Set.of(UserAccount.ROLE_USER, UserAccount.ROLE_ADMIN));
            createUser(account);
        } else if (!account.getRoles().contains(UserAccount.ROLE_USER)
            || !account.getRoles().contains(UserAccount.ROLE_ADMIN)) {
            Set<String> roles = new java.util.HashSet<>(account.getRoles());
            roles.add(UserAccount.ROLE_USER);
            roles.add(UserAccount.ROLE_ADMIN);
            updateRoles(username, roles);
        }
        if (account != null && !account.isEnabled()) {
            updateEnabled(username, true);
        }
    }

    /**
     * Verifies that the required default administrator is valid.
     */
    private void validateDefaultAdminAccount() {
        UserAccount defaultAdmin = users.get(DEFAULT_ADMIN_USERNAME);
        if (defaultAdmin == null || !defaultAdmin.isDefaultAdmin()
            || !defaultAdmin.getRoles().contains(UserAccount.ROLE_USER)
            || !defaultAdmin.getRoles().contains(UserAccount.ROLE_ADMIN)) {
            throw new IllegalStateException("Default admin account is missing or invalid");
        }
    }
}
