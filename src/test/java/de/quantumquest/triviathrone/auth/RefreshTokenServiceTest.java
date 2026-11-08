package de.quantumquest.triviathrone.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

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
        assertTrue(persistedData.contains("<!DOCTYPE refreshSessions SYSTEM \"refresh-tokens.dtd\">"));
        assertTrue(persistedData.contains("noNamespaceSchemaLocation=\"refresh-tokens.xsd\""));
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
    void doesNotLoadLegacyJsonRefreshSessions() throws Exception {
        Path legacyFile = tempDir.resolve("refresh-tokens.json");
        Files.writeString(legacyFile, """
                {"sessions":[]}
                """);

        RefreshTokenService restartedInstance = newService();

        assertThrows(ResponseStatusException.class, () -> restartedInstance.rotate("legacy-token"));
        assertTrue(Files.exists(legacyFile));
    }

    @Test
    void rejectsRefreshXmlWithoutValidationDeclarations() throws Exception {
        long expiry = Instant.now().plusSeconds(3600).getEpochSecond();
        long absoluteExpiry = Instant.now().plusSeconds(7200).getEpochSecond();
        Files.writeString(sessionFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <refreshSessions>
                    <session tokenDigest="digest" username="player" expiresAtEpochSecond="%d"
                             absoluteExpiryEpochSecond="%d"/>
                </refreshSessions>
                """.formatted(expiry, absoluteExpiry));

        assertThrows(IllegalStateException.class, this::newService);
    }

    @Test
    void rejectsRefreshXmlThatDoesNotMatchItsSchemas() throws Exception {
        Files.writeString(sessionFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE refreshSessions SYSTEM "refresh-tokens.dtd">
                <refreshSessions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                 xsi:noNamespaceSchemaLocation="refresh-tokens.xsd">
                    <session tokenDigest="digest" expiresAtEpochSecond="123" absoluteExpiryEpochSecond="456"/>
                </refreshSessions>
                """);

        assertThrows(IllegalStateException.class, this::newService);
    }

    private RefreshTokenService newService() {
        AuthProperties properties = new AuthProperties(
                tempDir.resolve("users.xml"),
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
