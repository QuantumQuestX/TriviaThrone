package quantumquest.triviathrone.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        String jwtSecret,
        long tokenValiditySeconds,
        long refreshTokenValiditySeconds,
        long refreshSessionMaxSeconds,
        boolean refreshCookieSecure,
        String initialAdminUsername,
        String initialAdminPassword
) {
    public AuthProperties {
        String signingSecret = Objects.requireNonNull(jwtSecret, "jwtSecret");
        if (signingSecret.isBlank()) {
            throw new IllegalArgumentException("jwtSecret must not be blank");
        }
        if (tokenValiditySeconds <= 0 || refreshTokenValiditySeconds <= 0 || refreshSessionMaxSeconds <= 0) {
            throw new IllegalArgumentException("Token lifetimes must be positive");
        }
        boolean hasAdminUsername = initialAdminUsername != null && !initialAdminUsername.isBlank();
        boolean hasAdminPassword = initialAdminPassword != null && !initialAdminPassword.isBlank();
        if (hasAdminUsername != hasAdminPassword) {
            throw new IllegalArgumentException("initialAdminUsername and initialAdminPassword must be configured together");
        }
    }
}
