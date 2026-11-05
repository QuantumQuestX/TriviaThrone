package de.quantumquest.triviathrone.auth;

import java.util.Objects;
import java.util.Set;

public class UserAccount {
    private String username;
    private String displayName;
    private String passwordHash;
    private Set<String> roles = Set.of();

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UserAccount other)) return false;
        return Objects.equals(username, other.username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username);
    }

    @Override
    public String toString() {
        return String.format("UserAccount[username=%s, displayName=%s, roles=%s]", username, displayName, roles);
    }
}
