package quantumquest.triviathrone.auth;

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
import quantumquest.triviathrone.dto.ChangePasswordRequest;
import quantumquest.triviathrone.dto.LoginRequest;
import quantumquest.triviathrone.dto.TokenResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Provides HTTP endpoints for authentication, token refresh, logout, and password changes.
 *
 * @author TiJ
 * @since 2.0.0
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
    /**
     * Holds the auth used by this component.
     */
    private final AuthService auth;
    /**
     * Holds the properties used by this component.
     */
    private final AuthProperties properties;

    /**
     * Creates a AuthController.
     *
     * @param auth       the auth
     * @param properties the configuration properties
     */
    public AuthController(AuthService auth, AuthProperties properties) {
        this.auth = Objects.requireNonNull(auth, "auth");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    /**
     * Prevents authentication responses from being cached.
     */
    private static void setNoStore(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
    }

    /**
     * Authenticates credentials and writes a refresh-token cookie for the new session.
     *
     * @param request      the request payload
     * @param refreshToken the refresh token
     * @param response     the HTTP response
     * @return the authenticated session
     */
    @PostMapping("/login")
    public TokenResponse login(
            @RequestBody LoginRequest request,
            @CookieValue(name = RefreshTokenService.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        LoginRequest loginRequest = Objects.requireNonNull(request, "request");
        AuthService.Session session = auth.authenticate(loginRequest.username(), loginRequest.password());
        auth.logout(refreshToken);
        return writeSession(session, response);
    }

    /**
     * Rotates the refresh-token cookie and returns a new access token.
     *
     * @param refreshToken the refresh token
     * @param response     the HTTP response
     * @return the rotated refresh-token session
     */
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

    /**
     * Revokes the presented refresh token and clears its cookie.
     *
     * @param refreshToken the refresh token
     * @param response     the HTTP response
     */
    @PostMapping("/logout")
    public void logout(
            @CookieValue(name = RefreshTokenService.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        auth.logout(refreshToken);
        clearRefreshCookie(response);
        setNoStore(response);
    }

    /**
     * Changes the authenticated user's password and invalidates their sessions.
     *
     * @param request        the request payload
     * @param authentication the current authentication
     */
    @PostMapping("/change-password")
    public void changePassword(@RequestBody ChangePasswordRequest request, Authentication authentication) {
        ChangePasswordRequest passwordRequest = Objects.requireNonNull(request, "request");
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        if (!(authentication.getPrincipal() instanceof UserAccount user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        auth.changePassword(
                user.getUsername(),
                passwordRequest.oldPassword(),
                passwordRequest.newPassword()
        );
    }

    /**
     * Writes the refresh-token cookie and returns the access-token response.
     */
    private TokenResponse writeSession(AuthService.Session session, HttpServletResponse response) {
        long remainingSeconds = Math.max(
                0,
                Duration.between(Instant.now(), session.refreshExpiresAt()).getSeconds()
        );
        ResponseCookie cookie = ResponseCookie.from(RefreshTokenService.COOKIE_NAME, session.refreshToken())
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Strict")
                .path("/auth")
                .maxAge(Duration.ofSeconds(remainingSeconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        setNoStore(response);
        return new TokenResponse(session.accessToken(), session.expiresIn(), session.userId());
    }

    /**
     * Expires the refresh-token cookie on the response.
     */
    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(RefreshTokenService.COOKIE_NAME, "")
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Strict")
                .path("/auth")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        setNoStore(response);
    }
}
