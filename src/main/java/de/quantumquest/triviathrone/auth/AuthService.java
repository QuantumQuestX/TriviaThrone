package de.quantumquest.triviathrone.auth;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserConfigLoader users;
    private final PasswordEncoder encoder;
    private final TokenService tokens;

    public AuthService(UserConfigLoader users, TokenService tokens) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = new BCryptPasswordEncoder();
    }

    public String authenticate(String username, String password) {
        UserAccount user = users.findByUsername(username);

        if (user == null || !encoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return tokens.issueToken(user);
    }
}
