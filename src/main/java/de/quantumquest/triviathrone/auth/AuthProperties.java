package de.quantumquest.triviathrone.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        Path userFile,
        String jwtSecret,
        long tokenValiditySeconds,
        long refreshTokenValiditySeconds,
        long refreshSessionMaxSeconds,
        boolean refreshCookieSecure,
        String initialPassword
) {
}
