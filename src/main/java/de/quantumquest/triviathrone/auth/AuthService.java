package de.quantumquest.triviathrone.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class AuthService {
    private final UserConfigLoader users;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final TokenService tokens;
    private final RefreshTokenService refreshTokens;

    public AuthService(UserConfigLoader users, TokenService tokens, RefreshTokenService refreshTokens) {
        this.users = users;
        this.tokens = tokens;
        this.refreshTokens = refreshTokens;
    }

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

    public Session authenticate(String username, String password) {
        UserAccount user = users.findByUsername(username);

        if (user == null || !encoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return createSession(user, refreshTokens.issue(user.getUsername()));
    }

    public Session refresh(String refreshToken) {
        RefreshTokenService.IssuedToken rotated = refreshTokens.rotate(refreshToken);
        UserAccount user = users.findByUsername(rotated.username());
        if (user == null) {
            refreshTokens.revoke(rotated.value());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        return createSession(user, rotated);
    }

    public void logout(String refreshToken) {
        refreshTokens.revoke(refreshToken);
    }

    private Session createSession(UserAccount user, RefreshTokenService.IssuedToken refreshToken) {
        return new Session(
                tokens.issueToken(user),
                refreshToken.value(),
                tokens.getValiditySeconds(),
                refreshToken.expiresAt()
        );
    }

    public record Session(String accessToken, String refreshToken, long expiresIn, Instant refreshExpiresAt) {
    }
}
