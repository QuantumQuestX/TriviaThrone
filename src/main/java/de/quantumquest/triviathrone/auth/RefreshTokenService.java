package de.quantumquest.triviathrone.auth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class RefreshTokenService {
    public static final String COOKIE_NAME = "refresh_token";

    private final SecureRandom random = new SecureRandom();
    private final ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final long validitySeconds;
    private final long maxSessionSeconds;

    public RefreshTokenService(AuthProperties properties) {
        this.validitySeconds = properties.getRefreshTokenValiditySeconds();
        this.maxSessionSeconds = properties.getRefreshSessionMaxSeconds();
        if (validitySeconds <= 0 || maxSessionSeconds <= 0) {
            throw new IllegalArgumentException("Refresh token lifetimes must be positive.");
        }
    }

    public synchronized IssuedToken issue(String username) {
        Instant now = Instant.now();
        Instant absoluteExpiry = now.plusSeconds(maxSessionSeconds);
        return issue(username, min(now.plusSeconds(validitySeconds), absoluteExpiry), absoluteExpiry);
    }

    public synchronized IssuedToken rotate(String presentedToken) {
        if (presentedToken == null || presentedToken.isBlank()) {
            throw invalidToken();
        }

        Session session = sessions.remove(digest(presentedToken));
        Instant now = Instant.now();
        if (session == null || !now.isBefore(session.expiresAt())
                || !now.isBefore(session.absoluteExpiry())) {
            throw invalidToken();
        }

        Instant expiry = min(now.plusSeconds(validitySeconds), session.absoluteExpiry());
        return issue(session.username(), expiry, session.absoluteExpiry());
    }

    public synchronized void revoke(String token) {
        if (token != null && !token.isBlank()) {
            sessions.remove(digest(token));
        }
    }

    public synchronized void revokeAll(String username) {
        sessions.entrySet().removeIf(entry -> entry.getValue().username().equals(username));
    }

    private IssuedToken issue(String username, Instant expiry, Instant absoluteExpiry) {
        purgeExpired();

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(digest(token), new Session(username, expiry, absoluteExpiry));
        return new IssuedToken(username, token, expiry);
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
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

    private record Session(String username, Instant expiresAt, Instant absoluteExpiry) {
    }

    public record IssuedToken(String username, String value, Instant expiresAt) {
    }
}
