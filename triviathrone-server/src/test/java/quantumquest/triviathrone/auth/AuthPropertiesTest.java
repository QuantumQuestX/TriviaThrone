package quantumquest.triviathrone.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthPropertiesTest {
    @Test
    void rejectsMissingJwtSecret() {
        assertThrows(IllegalArgumentException.class, () -> properties(""));
    }

    @Test
    void rejectsExampleJwtSecret() {
        assertThrows(
                IllegalArgumentException.class,
                () -> properties("CHANCE_THIS_SECRET_CHANGE_THIS_SECRET")
        );
    }

    @Test
    void acceptsConfiguredJwtSecret() {
        assertDoesNotThrow(() -> properties("01234567890123456789012345678901"));
    }

    private AuthProperties properties(String jwtSecret) {
        return new AuthProperties(jwtSecret, 900, 3600, 7200, false, "", "");
    }
}
