package quantumquest.triviathrone.auth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Issues, rotates, revokes, and persists refresh-token sessions.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Service
public class RefreshTokenService {
    /**
     * Generates cryptographically secure random values.
     *
     * @param properties the configuration properties
     * @param sessionStore the persistent session store
     * @return the generated result
     */
    public static final String COOKIE_NAME = "refresh_token";

    /**
     * Generates cryptographically secure random values.
     */
    private final SecureRandom random = new SecureRandom();
    /**
     * Holds the sessions used by this component.
     */
    private final Map<String, RefreshTokenSession> sessions;
    /**
     * Holds the sessio tore used by this component.
     */
    private final RefreshTokenSessionStore sessionStore;
    /**
     * Holds the validit econds used by this component.
     */
    private final long validitySeconds;
    /**
     * Holds the ma essio econds used by this component.
     */
    private final long maxSessionSeconds;

    /**
     * Creates a RefreshTokenService.
     *
     * @param properties   the configuration properties
     * @param sessionStore the persistent session store
     */
    public RefreshTokenService(AuthProperties properties, RefreshTokenSessionStore sessionStore) {
        Objects.requireNonNull(properties, "properties");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.validitySeconds = properties.refreshTokenValiditySeconds();
        this.maxSessionSeconds = properties.refreshSessionMaxSeconds();
        this.sessions = new java.util.HashMap<>(sessionStore.load());
    }

    /**
     * Removes expired sessions from the supplied map.
     */
    private static void purgeExpired(Map<String, RefreshTokenSession> sessions, Instant now) {
        sessions.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt())
            || !now.isBefore(entry.getValue().absoluteExpiry()));
    }

    /**
     * Computes the URL-safe SHA-256 digest of a token.
     */
    private static String digest(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /**
     * Returns the earlier of two instants.
     */
    private static Instant min(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }

    /**
     * Creates the standard invalid-refresh-token response.
     */
    private static ResponseStatusException invalidToken() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    }

    /**
     * Issues and persists a refresh token for the supplied user.
     *
     * @param username the account username
     * @return the issued refresh token
     */
    public synchronized IssuedToken issue(String username) {
        Objects.requireNonNull(username, "username");
        Instant now = Instant.now();
        Instant absoluteExpiry = now.plusSeconds(maxSessionSeconds);
        Instant expiry = min(now.plusSeconds(validitySeconds), absoluteExpiry);
        IssuedToken token = createToken(username, expiry);

        Map<String, RefreshTokenSession> updatedSessions = new HashMap<>(sessions);
        purgeExpired(updatedSessions, now);
        updatedSessions.put(digest(token.value()), new RefreshTokenSession(username, expiry, absoluteExpiry));
        saveAndReplace(updatedSessions);
        return token;
    }

    /**
     * Atomically replaces a valid refresh token with a new token.
     *
     * @param presentedToken the client-provided refresh token
     * @return the generated result
     */
    public synchronized IssuedToken rotate(String presentedToken) {
        if (presentedToken == null || presentedToken.isBlank()) {
            throw invalidToken();
        }

        String presentedDigest = digest(presentedToken);
        RefreshTokenSession session = sessions.get(presentedDigest);
        Instant now = Instant.now();
        if (session == null || !now.isBefore(session.expiresAt())
            || !now.isBefore(session.absoluteExpiry())) {
            throw invalidToken();
        }

        Instant expiry = min(now.plusSeconds(validitySeconds), session.absoluteExpiry());
        IssuedToken token = createToken(session.username(), expiry);
        Map<String, RefreshTokenSession> updatedSessions = new HashMap<>(sessions);
        updatedSessions.remove(presentedDigest);
        purgeExpired(updatedSessions, now);
        updatedSessions.put(digest(token.value()),
            new RefreshTokenSession(session.username(), expiry, session.absoluteExpiry()));
        saveAndReplace(updatedSessions);
        return token;
    }

    /**
     * Revokes the supplied refresh token when it is present.
     *
     * @param token the token
     */
    public synchronized void revoke(String token) {
        if (token != null && !token.isBlank()) {
            Map<String, RefreshTokenSession> updatedSessions = new HashMap<>(sessions);
            if (updatedSessions.remove(digest(token)) != null) {
                saveAndReplace(updatedSessions);
            }
        }
    }

    /**
     * Revokes every refresh token belonging to the supplied user.
     *
     * @param username the account username
     */
    public synchronized void revokeAll(String username) {
        Objects.requireNonNull(username, "username");
        Map<String, RefreshTokenSession> updatedSessions = new HashMap<>(sessions);
        if (updatedSessions.entrySet().removeIf(entry -> entry.getValue().username().equals(username))) {
            saveAndReplace(updatedSessions);
        }
    }

    /**
     * Generates a random refresh token with the supplied expiry.
     */
    private IssuedToken createToken(String username, Instant expiry) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new IssuedToken(username, token, expiry);
    }

    /**
     * Persists replacement sessions and updates the in-memory cache.
     */
    private void saveAndReplace(Map<String, RefreshTokenSession> updatedSessions) {
        sessionStore.save(updatedSessions);
        sessions.clear();
        sessions.putAll(updatedSessions);
    }

    /**
     * Represents a newly issued refresh token.
     *
     * @param username  the token owner
     * @param value     the opaque token value
     * @param expiresAt the token expiry time
     * @author TiJ
     * @since 2.0.0
     */
    public record IssuedToken(String username, String value, Instant expiresAt) {
        /**
         * Validates the issued refresh-token values before they are persisted or returned.
         */
        public IssuedToken {
            Objects.requireNonNull(username, "username");
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(expiresAt, "expiresAt");
        }
    }
}
