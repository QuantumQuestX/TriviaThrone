package de.quantumquest.triviathrone.auth;

import de.quantumquest.triviathrone.exceptions.WeakPasswordException;

import java.util.List;
import java.util.function.Predicate;

public final class PasswordUtils {
    private static final List<PasswordRule> PASSWORD_RULES = List.of(
            new PasswordRule("Password must be at least 12 characters long",
                    password -> password != null && password.length() >= 12),
            new PasswordRule("Password must contain uppercase letters",
                    password -> password.chars().anyMatch(Character::isUpperCase)),
            new PasswordRule("Password must contain lowercase letters",
                    password -> password.chars().anyMatch(Character::isLowerCase)),
            new PasswordRule("Password must contain digits",
                    password -> password.chars().anyMatch(Character::isDigit)),
            new PasswordRule("Password must contain special characters",
                    password -> password.chars().anyMatch(c -> "!@#$%^&*()_+-=[]{}|;:',.<>/?".indexOf(c) >= 0))
    );

    public static void validate(String password) {
        for (PasswordRule rule : PASSWORD_RULES) {
            if (!rule.predicate().test(password)) {
                throw new WeakPasswordException(rule.message());
            }
        }
    }

    private record PasswordRule(String message, Predicate<String> predicate) {
    }
}
