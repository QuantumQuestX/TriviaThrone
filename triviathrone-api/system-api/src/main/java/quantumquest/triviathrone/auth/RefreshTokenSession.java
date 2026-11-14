package quantumquest.triviathrone.auth;

import java.time.Instant;
import java.util.Objects;

public record RefreshTokenSession(String username, Instant expiresAt, Instant absoluteExpiry) {
    public RefreshTokenSession {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(absoluteExpiry, "absoluteExpiry");
    }
}
