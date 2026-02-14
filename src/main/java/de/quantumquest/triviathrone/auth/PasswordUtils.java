package de.quantumquest.triviathrone.auth;

import de.quantumquest.triviathrone.exceptions.WeakPasswordException;

import java.util.Map;
import java.util.function.Predicate;

public final class PasswordUtils {
    private static final Map<Predicate<String>, String> passwordRules = Map.of(
            pw -> pw != null && pw.length() >= 12, "Password must be at least 12 characters long",
            pw -> pw.chars().anyMatch(Character::isUpperCase), "Password must contain uppercase letters",
            pw -> pw.chars().anyMatch(Character::isLowerCase), "Password must contain lowercase letters",
            pw -> pw.chars().anyMatch(Character::isDigit), "Password must contain digits",
            pw -> pw.chars().anyMatch(c -> "!@#$%^&*()_+-=[]{}|;:',.<>/?".indexOf(c) >= 0), "Password must contain special characters"
    );

    public static void validate(String password) {
        for (Predicate<String> rule : passwordRules.keySet()) {
            if (!rule.test(password)) {
                throw new WeakPasswordException(passwordRules.get(rule));
            }
        }
    }
}
