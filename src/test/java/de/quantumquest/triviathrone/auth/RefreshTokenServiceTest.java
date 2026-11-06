package de.quantumquest.triviathrone.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenServiceTest {
    @TempDir
    Path tempDir;

    private Path sessionFile;

    @BeforeEach
    void setUp() {
        sessionFile = tempDir.resolve("refresh-tokens.xml");
    }

    @Test
    void refreshSessionSurvivesServiceRestart() throws Exception {
        RefreshTokenService firstInstance = newService();
        RefreshTokenService.IssuedToken issuedToken = firstInstance.issue("player");

        String persistedData = Files.readString(sessionFile);
        assertTrue(persistedData.contains("<refreshSessions>"));
        assertFalse(persistedData.contains(issuedToken.value()));

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

    @Test
    void migratesExistingJsonRefreshSessionsToXml() throws Exception {
        String legacyToken = "legacy-token";
        String tokenDigest = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(legacyToken.getBytes(StandardCharsets.UTF_8))
        );
        long expiry = Instant.now().plusSeconds(3600).getEpochSecond();
        long absoluteExpiry = Instant.now().plusSeconds(7200).getEpochSecond();
        Path legacyFile = tempDir.resolve("refresh-tokens.json");
        Files.writeString(legacyFile, """
                {"sessions":[{"tokenDigest":"%s","username":"player","expiresAtEpochSecond":%d,
                "absoluteExpiryEpochSecond":%d}]}
                """.formatted(tokenDigest, expiry, absoluteExpiry));

        RefreshTokenService restartedInstance = newService();

        assertEquals("player", restartedInstance.rotate(legacyToken).username());
        assertFalse(Files.exists(legacyFile));
        assertTrue(Files.exists(sessionFile));
    }

    private RefreshTokenService newService() {
        AuthProperties properties = new AuthProperties(
                tempDir.resolve("users.yml"),
                sessionFile,
                "01234567890123456789012345678901",
                900,
                3600,
                7200,
                false,
                null
        );
        return new RefreshTokenService(properties, new RefreshTokenSessionStore(properties));
    }
}
