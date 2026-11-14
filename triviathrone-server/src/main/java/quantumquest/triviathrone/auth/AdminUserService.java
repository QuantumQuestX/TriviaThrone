package quantumquest.triviathrone.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import quantumquest.triviathrone.exceptions.WeakPasswordException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Applies validated administrative changes to user accounts and sessions.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Service
public class AdminUserService {
    /**
     * Holds the users used by this component.
     */
    private final UserConfigLoader users;
    /**
     * Holds the password encoder used by this component.
     */
    private final PasswordEncoder passwordEncoder;
    /**
     * Holds the refresh-token service used by this component.
     */
    private final RefreshTokenService refreshTokens;

    /**
     * Creates a AdminUserService.
     *
     * @param users           the user-account store
     * @param passwordEncoder the password encoder
     * @param refreshTokens   the refresh-token service
     */
    public AdminUserService(
            UserConfigLoader users,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokens
    ) {
        this.users = Objects.requireNonNull(users, "users");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
        this.refreshTokens = Objects.requireNonNull(refreshTokens, "refreshTokens");
    }

    /**
     * Validates and trims an account username.
     */
    private static String normalizeUsername(String username) {
        if (username == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }
        String normalized = username.trim();
        if (normalized.isEmpty() || normalized.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username must be between 1 and 255 characters");
        }
        return normalized;
    }

    /**
     * Normalizes a display name or falls back to the username.
     */
    private static String normalizeDisplayName(String displayName, String username) {
        if (displayName == null || displayName.isBlank()) {
            return username;
        }
        String normalized = displayName.trim();
        if (normalized.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name must be at most 255 characters");
        }
        return normalized;
    }

    /**
     * Validates and copies the requested roles.
     */
    private static Set<String> normalizeRoles(Set<String> requestedRoles) {
        Set<String> roles = requestedRoles == null ? new HashSet<>() : new HashSet<>(requestedRoles);
        if (roles.stream().anyMatch(role -> !UserAccount.ROLE_USER.equals(role)
                && !UserAccount.ROLE_ADMIN.equals(role))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Roles may only contain USER or ADMIN");
        }
        return Set.copyOf(roles);
    }

    /**
     * Returns summaries for every configured user account.
     *
     * @return summaries for every configured user account
     */
    public List<UserSummary> listUsers() {
        return users.findAll().stream().map(UserSummary::from).toList();
    }

    /**
     * Creates a validated user account.
     *
     * @param request the request payload
     * @return the created a validated user account
     */
    public synchronized UserSummary createUser(CreateUserRequest request) {
        CreateUserRequest userRequest = Objects.requireNonNull(request, "request");
        String username = normalizeUsername(userRequest.username());
        String password = userRequest.password();
        if (password == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        try {
            PasswordUtils.validate(password);
        } catch (WeakPasswordException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setDisplayName(normalizeDisplayName(userRequest.displayName(), username));
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRoles(normalizeRoles(userRequest.roles()));
        if (users.findByUsername(username) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use");
        }
        users.createUser(user);
        reconcileDefaultAdmin();
        return UserSummary.from(user);
    }

    /**
     * Persists and applies an account's roles.
     *
     * @param username the account username
     * @param roles    the requested account roles
     * @return the updated account summary
     */
    public synchronized UserSummary updateRoles(String username, Set<String> roles) {
        UserAccount user = requireUser(username);
        Set<String> updatedRoles = normalizeRoles(roles);
        protectDefaultAdmin(user, updatedRoles);
        if (!user.getRoles().equals(updatedRoles)) {
            users.updateRoles(username, updatedRoles);
            refreshTokens.revokeAll(username);
            reconcileDefaultAdmin();
        }
        return UserSummary.from(user);
    }

    /**
     * Deletes a non-default account.
     *
     * @param username the account username
     */
    public synchronized void deleteUser(String username) {
        UserAccount user = requireUser(username);
        if (user.isDefaultAdmin()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The default admin account cannot be deleted");
        }
        protectDefaultAdmin(user, Set.of());
        users.deleteUser(username);
        refreshTokens.revokeAll(username);
        reconcileDefaultAdmin();
    }

    /**
     * Finds an existing user account or reports that it is absent.
     */
    private UserAccount requireUser(String username) {
        UserAccount user = users.findByUsername(Objects.requireNonNull(username, "username"));
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return user;
    }

    /**
     * Rejects changes that would remove required default-administrator roles.
     */
    private void protectDefaultAdmin(UserAccount user, Set<String> updatedRoles) {
        if (user.isDefaultAdmin() && (!updatedRoles.contains(UserAccount.ROLE_USER)
                || !updatedRoles.contains(UserAccount.ROLE_ADMIN))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "The default admin roles are managed automatically");
        }
    }

    /**
     * Persists and applies an account's enabled state.
     *
     * @param username the account username
     * @param enabled  the requested enabled state
     * @return the updated account summary
     */
    public synchronized UserSummary updateEnabled(String username, boolean enabled) {
        UserAccount user = requireUser(username);
        if (user.isEnabled() == enabled) {
            return UserSummary.from(user);
        }
        if (user.isDefaultAdmin()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "The default admin enabled state is managed automatically");
        }
        users.updateEnabled(username, enabled);
        if (!enabled) {
            refreshTokens.revokeAll(username);
        }
        reconcileDefaultAdmin();
        return UserSummary.from(user);
    }

    /**
     * Reconciles the default administrator enabled state.
     */
    private void reconcileDefaultAdmin() {
        UserAccount defaultAdmin = users.findByUsername(UserConfigLoader.DEFAULT_ADMIN_USERNAME);
        boolean wasEnabled = defaultAdmin != null && defaultAdmin.isEnabled();
        users.reconcileDefaultAdmin();
        if (wasEnabled && !defaultAdmin.isEnabled()) {
            refreshTokens.revokeAll(defaultAdmin.getUsername());
        }
    }

    /**
     * Carries values for a new account.
     *
     * @param username    the account username
     * @param displayName the account display name
     * @param password    the initial plaintext password
     * @param roles       the initial role set
     * @author TiJ
     * @since 2.0.0
     */
    public record CreateUserRequest(String username, String displayName, String password, Set<String> roles) {
    }

    /**
     * Summarizes an account for administrative clients.
     *
     * @param userId       the stable account identifier
     * @param username     the account username
     * @param displayName  the account display name
     * @param roles        the assigned roles
     * @param enabled      whether the account can authenticate
     * @param defaultAdmin whether the account is the protected default administrator
     * @author TiJ
     * @since 2.0.0
     */
    public record UserSummary(
            String userId,
            String username,
            String displayName,
            Set<String> roles,
            boolean enabled,
            boolean defaultAdmin
    ) {
        /**
         * Builds a summary from a user account.
         */
        private static UserSummary from(UserAccount user) {
            return new UserSummary(
                user.getUserId(), user.getUsername(), user.getDisplayName(), user.getRoles(),
                user.isEnabled(), user.isDefaultAdmin()
            );
        }
    }
}
