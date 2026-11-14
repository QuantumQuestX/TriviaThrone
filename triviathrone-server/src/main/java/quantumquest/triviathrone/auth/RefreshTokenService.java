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

@Service
public class RefreshTokenService {
    public static final String COOKIE_NAME = "refresh_token";

    private final SecureRandom random = new SecureRandom();
    private final Map<String, RefreshTokenSession> sessions;
    private final RefreshTokenSessionStore sessionStore;
    private final long validitySeconds;
    private final long maxSessionSeconds;

    public RefreshTokenService(AuthProperties properties, RefreshTokenSessionStore sessionStore) {
        Objects.requireNonNull(properties, "properties");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.validitySeconds = properties.refreshTokenValiditySeconds();
        this.maxSessionSeconds = properties.refreshSessionMaxSeconds();
        this.sessions = new java.util.HashMap<>(sessionStore.load());
    }

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

    public synchronized void revoke(String token) {
        if (token != null && !token.isBlank()) {
            Map<String, RefreshTokenSession> updatedSessions = new HashMap<>(sessions);
            if (updatedSessions.remove(digest(token)) != null) {
                saveAndReplace(updatedSessions);
            }
        }
    }

    public synchronized void revokeAll(String username) {
        Objects.requireNonNull(username, "username");
        Map<String, RefreshTokenSession> updatedSessions = new HashMap<>(sessions);
        if (updatedSessions.entrySet().removeIf(entry -> entry.getValue().username().equals(username))) {
            saveAndReplace(updatedSessions);
        }
    }

    private IssuedToken createToken(String username, Instant expiry) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new IssuedToken(username, token, expiry);
    }

    private void saveAndReplace(Map<String, RefreshTokenSession> updatedSessions) {
        sessionStore.save(updatedSessions);
        sessions.clear();
        sessions.putAll(updatedSessions);
    }

    private static void purgeExpired(Map<String, RefreshTokenSession> sessions, Instant now) {
        sessions.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt())
                || !now.isBefore(entry.getValue().absoluteExpiry()));
    }

    private static String digest(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static Instant min(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }

    private static ResponseStatusException invalidToken() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    }

    public record IssuedToken(String username, String value, Instant expiresAt) {
        public IssuedToken {
            Objects.requireNonNull(username, "username");
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(expiresAt, "expiresAt");
        }
    }
}
