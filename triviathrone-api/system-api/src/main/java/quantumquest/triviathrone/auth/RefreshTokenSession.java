package quantumquest.triviathrone.auth;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable refresh-token session metadata.
 *
 * @param username       authenticated username
 * @param expiresAt      rolling session expiration instant
 * @param absoluteExpiry maximum non-extendable expiration instant
 * @author TiJ
 * @since 2.0.0
 */
public record RefreshTokenSession(String username, Instant expiresAt, Instant absoluteExpiry) {
    /**
     * Validates the non-null session metadata.
     */
    public RefreshTokenSession {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(absoluteExpiry, "absoluteExpiry");
    }
}
