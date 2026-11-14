package quantumquest.triviathrone.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

/**
 * Validated configuration properties for JWT and refresh-token authentication.
 *
 * @param jwtSecret                   the HMAC signing secret
 * @param tokenValiditySeconds        the access-token lifetime in seconds
 * @param refreshTokenValiditySeconds the refresh-token lifetime in seconds
 * @param refreshSessionMaxSeconds    the maximum refresh-session lifetime in seconds
 * @param refreshCookieSecure         whether refresh cookies require HTTPS
 * @param initialAdminUsername        the optional initial administrator username
 * @param initialAdminPassword        the optional initial administrator password
 * @author TiJ
 * @since 2.0.0
 */
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
    /**
     * Validates required signing material, token lifetimes, and optional initial-admin credentials.
     */
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
