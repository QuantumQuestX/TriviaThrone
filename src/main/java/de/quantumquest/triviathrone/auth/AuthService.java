package de.quantumquest.triviathrone.auth;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final UserConfigLoader users;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final TokenService tokens;

    public AuthService(UserConfigLoader users, TokenService tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        UserAccount user = users.findByUsername(username);

        if (user == null || !encoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        PasswordUtils.validate(newPassword);

        String newHash = encoder.encode(newPassword);
        users.updatePassword(username, newHash);
    }

    public String authenticate(String username, String password) {
        UserAccount user = users.findByUsername(username);

        if (user == null || !encoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return tokens.issueToken(user);
    }
}
