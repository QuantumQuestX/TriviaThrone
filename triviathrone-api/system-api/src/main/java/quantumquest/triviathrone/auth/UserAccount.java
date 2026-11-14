package quantumquest.triviathrone.auth;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Mutable user account with immutable role snapshots and volatile authorization state.
 * Instances are not generally thread-safe for compound updates.
 *
 * @author TiJ
 * @since 2.0.0
 */
public class UserAccount {
    /**
     * Standard role granted to ordinary authenticated users.
     */
    public static final String ROLE_USER = "USER";
    /**
     * Administrative role granting account-management privileges.
     */
    public static final String ROLE_ADMIN = "ADMIN";

    /**
     * Pattern for a dashless RFC 4122 version-4 UUID.
     */
    private static final String USER_ID_PATTERN = "[0-9a-f]{12}4[0-9a-f]{3}[89ab][0-9a-f]{15}";
    /**
     * Generated immutable-format account identifier.
     */
    private String userId = UUID.randomUUID().toString().replace("-", "");
    /**
     * Login name, which may be {@code null} before initialization.
     */
    private String username;
    /**
     * Display name, which may be {@code null}.
     */
    private String displayName;
    /**
     * Stored password hash, which may be {@code null}.
     */
    private String passwordHash;
    /**
     * Immutable snapshot of assigned roles.
     */
    private volatile Set<String> roles = Set.of(ROLE_USER);
    /**
     * Whether the account may authenticate.
     */
    private volatile boolean enabled = true;
    /**
     * Whether this account is the bootstrap administrator.
     */
    private volatile boolean defaultAdmin;

    /**
     * Returns the dashless version-4 account identifier.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the dashless version-4 account identifier.
     *
     * @param userId non-null dashless UUID v4
     * @throws IllegalArgumentException if the identifier is not a dashless UUID v4
     */
    public void setUserId(String userId) {
        String id = Objects.requireNonNull(userId, "userId");
        if (!id.matches(USER_ID_PATTERN)) {
            throw new IllegalArgumentException("userId must be a dashless UUID v4");
        }
        this.userId = id;
    }

    /**
     * Returns the login name, possibly {@code null}.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the login name; {@code null} is permitted.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the display name, possibly {@code null}.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the display name; {@code null} is permitted.
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the stored password hash, possibly {@code null}.
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Sets the stored password hash; {@code null} is permitted.
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Returns the immutable snapshot of assigned roles.
     */
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Sets roles from a defensive copy; {@code null} becomes an empty role set.
     */
    public void setRoles(Set<String> roles) {
        Set<String> assignedRoles = roles == null ? new HashSet<>() : new HashSet<>(roles);
        this.roles = Set.copyOf(assignedRoles);
    }

    /**
     * Returns whether the account may authenticate.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether the account may authenticate.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns whether this account is the bootstrap administrator.
     */
    public boolean isDefaultAdmin() {
        return defaultAdmin;
    }

    /**
     * Sets whether this account is the bootstrap administrator.
     */
    public void setDefaultAdmin(boolean defaultAdmin) {
        this.defaultAdmin = defaultAdmin;
    }

    /**
     * Compares accounts by username.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof UserAccount other)) {
            return false;
        }
        return Objects.equals(username, other.username);
    }

    /**
     * Returns the username-based hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(username);
    }

    /**
     * Returns a password-safe account summary.
     */
    @Override
    public String toString() {
        return String.format("UserAccount[username=%s, displayName=%s, roles=%s]", username, displayName, roles);
    }
}
