package de.quantumquest.triviathrone.auth;

import de.quantumquest.triviathrone.dto.LoginRequest;
import de.quantumquest.triviathrone.dto.TokenResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public TokenResponse login(LoginRequest request) {
        return new TokenResponse(
                auth.authenticate(request.username(), request.password())
        );
    }
}
