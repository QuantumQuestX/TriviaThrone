package de.quantumquest.triviathrone.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.Objects;

@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        Path userMigrationFile,
        Path refreshTokenFile,
        String jwtSecret,
        long tokenValiditySeconds,
        long refreshTokenValiditySeconds,
        long refreshSessionMaxSeconds,
        boolean refreshCookieSecure,
        String initialPassword
) {
    public AuthProperties {
        Objects.requireNonNull(userMigrationFile, "userMigrationFile");
        Objects.requireNonNull(refreshTokenFile, "refreshTokenFile");
        String signingSecret = Objects.requireNonNull(jwtSecret, "jwtSecret");
        if (signingSecret.isBlank()) {
            throw new IllegalArgumentException("jwtSecret must not be blank");
        }
        if (tokenValiditySeconds <= 0 || refreshTokenValiditySeconds <= 0 || refreshSessionMaxSeconds <= 0) {
            throw new IllegalArgumentException("Token lifetimes must be positive");
        }
    }
}
