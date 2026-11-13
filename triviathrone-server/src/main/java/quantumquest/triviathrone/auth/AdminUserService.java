package quantumquest.triviathrone.auth;

import quantumquest.triviathrone.exceptions.WeakPasswordException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class AdminUserService {
    private final UserConfigLoader users;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokens;

    public AdminUserService(
            UserConfigLoader users,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokens
    ) {
        this.users = Objects.requireNonNull(users, "users");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
        this.refreshTokens = Objects.requireNonNull(refreshTokens, "refreshTokens");
    }

    public List<UserSummary> listUsers() {
        return users.findAll().stream().map(UserSummary::from).toList();
    }

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

    private UserAccount requireUser(String username) {
        UserAccount user = users.findByUsername(Objects.requireNonNull(username, "username"));
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return user;
    }

    private void protectDefaultAdmin(UserAccount user, Set<String> updatedRoles) {
        if (user.isDefaultAdmin() && (!updatedRoles.contains(UserAccount.ROLE_USER)
                || !updatedRoles.contains(UserAccount.ROLE_ADMIN))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The default admin roles are managed automatically");
        }
    }

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

    private void reconcileDefaultAdmin() {
        UserAccount defaultAdmin = users.findByUsername(UserConfigLoader.DEFAULT_ADMIN_USERNAME);
        boolean wasEnabled = defaultAdmin != null && defaultAdmin.isEnabled();
        users.reconcileDefaultAdmin();
        if (wasEnabled && !defaultAdmin.isEnabled()) {
            refreshTokens.revokeAll(defaultAdmin.getUsername());
        }
    }

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

    private static Set<String> normalizeRoles(Set<String> requestedRoles) {
        Set<String> roles = requestedRoles == null ? new HashSet<>() : new HashSet<>(requestedRoles);
        if (roles.stream().anyMatch(role -> !UserAccount.ROLE_USER.equals(role)
                && !UserAccount.ROLE_ADMIN.equals(role))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Roles may only contain USER or ADMIN");
        }
        return Set.copyOf(roles);
    }

    public record CreateUserRequest(String username, String displayName, String password, Set<String> roles) {
    }

    public record UserSummary(
            String userId,
            String username,
            String displayName,
            Set<String> roles,
            boolean enabled,
            boolean defaultAdmin
    ) {
        private static UserSummary from(UserAccount user) {
            return new UserSummary(
                    user.getUserId(), user.getUsername(), user.getDisplayName(), user.getRoles(),
                    user.isEnabled(), user.isDefaultAdmin()
            );
        }
    }
}
