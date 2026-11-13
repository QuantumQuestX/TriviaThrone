package quantumquest.triviathrone.auth;

import java.time.Instant;
import java.util.Objects;

record RefreshTokenSession(String username, Instant expiresAt, Instant absoluteExpiry) {
    RefreshTokenSession {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(absoluteExpiry, "absoluteExpiry");
    }
}
