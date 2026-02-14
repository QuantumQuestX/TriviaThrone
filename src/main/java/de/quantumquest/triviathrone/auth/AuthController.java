package de.quantumquest.triviathrone.auth;

import de.quantumquest.triviathrone.dto.ChangePasswordRequest;
import de.quantumquest.triviathrone.dto.LoginRequest;
import de.quantumquest.triviathrone.dto.TokenResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        return new TokenResponse(
                auth.authenticate(request.username(), request.password())
        );
    }

    @PostMapping("/change-password")
    public void changePassword(@RequestBody ChangePasswordRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        String username = ((UserAccount) authentication.getPrincipal()).getUsername();
        auth.changePassword(
                username,
                request.oldPassword(),
                request.newPassword()
        );
    }
}
