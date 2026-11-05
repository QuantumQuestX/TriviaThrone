package de.quantumquest.triviathrone.auth;

import de.quantumquest.triviathrone.dto.ChangePasswordRequest;
import de.quantumquest.triviathrone.dto.LoginRequest;
import de.quantumquest.triviathrone.dto.TokenResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService auth;
    private final AuthProperties properties;

    public AuthController(AuthService auth, AuthProperties properties) {
        this.auth = auth;
        this.properties = properties;
    }

    @PostMapping("/login")
    public TokenResponse login(
            @RequestBody LoginRequest request,
            @CookieValue(name = RefreshTokenService.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        AuthService.Session session = auth.authenticate(request.username(), request.password());
        auth.logout(refreshToken);
        return writeSession(session, response);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(
            @CookieValue(name = RefreshTokenService.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        try {
            return writeSession(auth.refresh(refreshToken), response);
        } catch (ResponseStatusException exception) {
            clearRefreshCookie(response);
            throw exception;
        }
    }

    @PostMapping("/logout")
    public void logout(
            @CookieValue(name = RefreshTokenService.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        auth.logout(refreshToken);
        clearRefreshCookie(response);
        setNoStore(response);
    }

    @PostMapping("/change-password")
    public void changePassword(@RequestBody ChangePasswordRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        if (!(authentication.getPrincipal() instanceof UserAccount user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        auth.changePassword(
                user.getUsername(),
                request.oldPassword(),
                request.newPassword()
        );
    }

    private TokenResponse writeSession(AuthService.Session session, HttpServletResponse response) {
        long remainingSeconds = Math.max(
                0,
                Duration.between(Instant.now(), session.refreshExpiresAt()).getSeconds()
        );
        ResponseCookie cookie = ResponseCookie.from(RefreshTokenService.COOKIE_NAME, session.refreshToken())
                .httpOnly(true)
                .secure(properties.isRefreshCookieSecure())
                .sameSite("Strict")
                .path("/auth")
                .maxAge(Duration.ofSeconds(remainingSeconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        setNoStore(response);
        return new TokenResponse(session.accessToken(), session.expiresIn());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(RefreshTokenService.COOKIE_NAME, "")
                .httpOnly(true)
                .secure(properties.isRefreshCookieSecure())
                .sameSite("Strict")
                .path("/auth")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        setNoStore(response);
    }

    private static void setNoStore(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
    }
}
