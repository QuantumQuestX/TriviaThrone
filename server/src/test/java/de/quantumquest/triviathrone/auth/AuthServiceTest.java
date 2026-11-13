package de.quantumquest.triviathrone.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private final UserConfigLoader users = mock(UserConfigLoader.class);
    private final TokenService tokens = mock(TokenService.class);
    private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuthService service = new AuthService(users, tokens, refreshTokens, passwordEncoder);

    @Test
    void rejectsPasswordLoginWhenUserRoleIsMissing() {
        UserAccount account = account(Set.of(UserAccount.ROLE_ADMIN));
        when(users.findByUsername("player")).thenReturn(account);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.authenticate("player", "password")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(passwordEncoder, never()).matches("password", account.getPasswordHash());
        verify(refreshTokens, never()).issue("player");
    }

    @Test
    void revokesRotatedRefreshTokenWhenUserRoleIsMissing() {
        RefreshTokenService.IssuedToken rotated = new RefreshTokenService.IssuedToken(
                "player", "rotated-token", Instant.now().plusSeconds(600)
        );
        when(refreshTokens.rotate("old-token")).thenReturn(rotated);
        when(users.findByUsername("player")).thenReturn(account(Set.of()));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.refresh("old-token")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(refreshTokens).revoke("rotated-token");
    }

    private static UserAccount account(Set<String> roles) {
        UserAccount account = new UserAccount();
        account.setUsername("player");
        account.setPasswordHash("encoded-password");
        account.setRoles(roles);
        return account;
    }
}
