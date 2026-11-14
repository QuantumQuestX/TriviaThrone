package quantumquest.triviathrone.auth;

import quantumquest.triviathrone.exceptions.WeakPasswordException;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Validates passwords against the application's strength requirements.
 *
 * @author TiJ
 * @since 2.0.0
 */
public final class PasswordUtils {
    /**
     * Defines the password strength rules.
     */
    private static final List<PasswordRule> PASSWORD_RULES = List.of(
        new PasswordRule("Password must be at least 12 characters long",
            password -> password.length() >= 12),
        new PasswordRule("Password must contain uppercase letters",
            password -> password.chars().anyMatch(Character::isUpperCase)),
        new PasswordRule("Password must contain lowercase letters",
            password -> password.chars().anyMatch(Character::isLowerCase)),
        new PasswordRule("Password must contain digits",
            password -> password.chars().anyMatch(Character::isDigit)),
        new PasswordRule("Password must contain special characters",
            password -> password.chars().anyMatch(c -> "!@#$%^&*()_+-=[]{}|;:',.<>/?".indexOf(c) >= 0))
    );

    /**
     * Validates a password against every configured strength rule.
     *
     * @param password the plaintext password
     */
    public static void validate(String password) {
        Objects.requireNonNull(password, "password");
        for (PasswordRule rule : PASSWORD_RULES) {
            if (!rule.predicate().test(password)) {
                throw new WeakPasswordException(rule.message());
            }
        }
    }

    /**
     * Represents a password-strength rule and its predicate.
     *
     * @param message   the validation failure message
     * @param predicate the test applied to a password
     * @author TiJ
     * @since 2.0.0
     */
    private record PasswordRule(String message, Predicate<String> predicate) {
    }
}
