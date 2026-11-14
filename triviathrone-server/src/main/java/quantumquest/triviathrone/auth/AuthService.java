package quantumquest.triviathrone.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Objects;

/**
 * Authenticates users and manages access and refresh-token sessions.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Service
public class AuthService {
    /**
     * Holds the users used by this component.
     */
    private final UserConfigLoader users;
    /**
     * Holds the encoder used by this component.
     */
    private final PasswordEncoder encoder;
    /**
     * Holds the tokens used by this component.
     */
    private final TokenService tokens;
    /**
     * Holds the refresh-token service used by this component.
     */
    private final RefreshTokenService refreshTokens;

    /**
     * Creates a AuthService.
     *
     * @param users         the user-account store
     * @param tokens        the JWT service
     * @param refreshTokens the refresh-token service
     * @param encoder       the password encoder
     */
    public AuthService(
            UserConfigLoader users,
            TokenService tokens,
            RefreshTokenService refreshTokens,
            PasswordEncoder encoder
    ) {
        this.users = Objects.requireNonNull(users, "users");
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.refreshTokens = Objects.requireNonNull(refreshTokens, "refreshTokens");
        this.encoder = Objects.requireNonNull(encoder, "encoder");
    }

    /**
     * Changes the authenticated user's password and invalidates their sessions.
     *
     * @param username    the account username
     * @param oldPassword the current plaintext password
     * @param newPassword the replacement plaintext password
     */
    public void changePassword(String username, String oldPassword, String newPassword) {
        UserAccount user = users.findByUsername(username);

        if (user == null || !encoder.matches(oldPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        PasswordUtils.validate(newPassword);

        String newHash = encoder.encode(newPassword);
        users.updatePassword(username, newHash);
        refreshTokens.revokeAll(username);
    }

    /**
     * Authenticates credentials and creates an access and refresh-token session.
     *
     * @param username the account username
     * @param password the plaintext password
     * @return the authenticated session
     */
    public Session authenticate(String username, String password) {
        UserAccount user = users.findByUsername(username);

        if (user == null || !user.isEnabled() || !user.getRoles().contains(UserAccount.ROLE_USER)
                || !encoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return createSession(user, refreshTokens.issue(user.getUsername()));
    }

    /**
     * Rotates the refresh-token cookie and returns a new access token.
     *
     * @param refreshToken the refresh token
     * @return the rotated refresh-token session
     */
    public Session refresh(String refreshToken) {
        RefreshTokenService.IssuedToken rotated = refreshTokens.rotate(refreshToken);
        UserAccount user = users.findByUsername(rotated.username());
        if (user == null || !user.isEnabled() || !user.getRoles().contains(UserAccount.ROLE_USER)) {
            refreshTokens.revoke(rotated.value());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        return createSession(user, rotated);
    }

    /**
     * Revokes the presented refresh token and clears its cookie.
     *
     * @param refreshToken the refresh token
     */
    public void logout(String refreshToken) {
        refreshTokens.revoke(refreshToken);
    }

    /**
     * Builds the client session for an authenticated user.
     */
    private Session createSession(UserAccount user, RefreshTokenService.IssuedToken refreshToken) {
        return new Session(
            tokens.issueToken(user),
            refreshToken.value(),
            tokens.getValiditySeconds(),
            refreshToken.expiresAt(),
            user.getUserId()
        );
    }

    /**
     * Represents the tokens and expiry data for an authenticated session.
     *
     * @param accessToken      the signed access token
     * @param refreshToken     the opaque refresh token
     * @param expiresIn        the access-token lifetime in seconds
     * @param refreshExpiresAt the refresh-token expiry time
     * @param userId           the authenticated user identifier
     * @author TiJ
     * @since 2.0.0
     */
    public record Session(
            String accessToken,
            String refreshToken,
            long expiresIn,
            Instant refreshExpiresAt,
            String userId
    ) {
        /**
         * Validates the session values returned to the authentication controller.
         */
        public Session {
            Objects.requireNonNull(accessToken, "accessToken");
            Objects.requireNonNull(refreshToken, "refreshToken");
            Objects.requireNonNull(refreshExpiresAt, "refreshExpiresAt");
            Objects.requireNonNull(userId, "userId");
        }
    }
}
