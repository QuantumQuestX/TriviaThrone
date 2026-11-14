package quantumquest.triviathrone.auth;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefreshTokenServiceTest {
    private final MemorySessionRepository repository = new MemorySessionRepository();

    @Test
    void refreshSessionSurvivesServiceRestartAndStoresOnlyDigest() {
        RefreshTokenService firstInstance = newService();
        RefreshTokenService.IssuedToken issuedToken = firstInstance.issue("player");

        String storedDigest = repository.sessions.keySet().iterator().next();
        org.junit.jupiter.api.Assertions.assertNotEquals(issuedToken.value(), storedDigest);

        RefreshTokenService restartedInstance = newService();
        RefreshTokenService.IssuedToken rotatedToken = restartedInstance.rotate(issuedToken.value());

        assertEquals("player", rotatedToken.username());
        assertThrows(ResponseStatusException.class, () -> restartedInstance.rotate(issuedToken.value()));
    }

    @Test
    void revokeSurvivesServiceRestart() {
        RefreshTokenService firstInstance = newService();
        RefreshTokenService.IssuedToken issuedToken = firstInstance.issue("player");
        firstInstance.revoke(issuedToken.value());

        RefreshTokenService restartedInstance = newService();

        assertThrows(ResponseStatusException.class, () -> restartedInstance.rotate(issuedToken.value()));
    }

    private RefreshTokenService newService() {
        AuthProperties properties = new AuthProperties(
                "01234567890123456789012345678901",
                900,
                3600,
                7200,
                false,
                "",
                ""
        );
        return new RefreshTokenService(properties, new RefreshTokenSessionStore(repository));
    }

    private static final class MemorySessionRepository implements IRefreshTokenSessionRepository {
        private final Map<String, RefreshTokenSession> sessions = new HashMap<>();

        @Override
        public Map<String, RefreshTokenSession> load() {
            return new HashMap<>(sessions);
        }

        @Override
        public void save(Map<String, RefreshTokenSession> updatedSessions) {
            sessions.clear();
            sessions.putAll(updatedSessions);
        }
    }
}
