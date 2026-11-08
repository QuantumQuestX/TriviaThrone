package de.quantumquest.triviathrone.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserConfigFileStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void doesNotLoadLegacyYamlUsers() throws Exception {
        Path xmlFile = tempDir.resolve("users.xml");
        Path yamlFile = tempDir.resolve("users.yml");
        Files.writeString(yamlFile, """
                users:
                  - username: player
                    displayName: "Trivia Player"
                    passwordHash: existing-hash
                """);
        AuthProperties properties = properties(xmlFile);

        assertThrows(IllegalStateException.class, () -> new UserConfigFileStore(properties).load());
        assertTrue(Files.exists(yamlFile));
        assertFalse(Files.exists(xmlFile));
    }

    @Test
    void rejectsInvalidUserXml() throws Exception {
        Path xmlFile = tempDir.resolve("users.xml");
        Files.writeString(xmlFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE users SYSTEM "users.dtd">
                <users xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:noNamespaceSchemaLocation="users.xsd">
                    <user><displayName>Missing username</displayName></user>
                </users>
                """);

        UserConfigFileStore store = new UserConfigFileStore(properties(xmlFile));

        assertThrows(IllegalStateException.class, store::load);
    }

    @Test
    void hashesMissingPasswordsFromValidatedXml() throws Exception {
        Path xmlFile = tempDir.resolve("users.xml");
        Files.writeString(xmlFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE users SYSTEM "users.dtd">
                <users xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:noNamespaceSchemaLocation="users.xsd">
                    <user><username>player</username></user>
                </users>
                """);
        AuthProperties properties = properties(xmlFile, "TestPassword#123");
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        UserConfigLoader users = new UserConfigLoader(
                properties,
                passwordEncoder,
                new UserConfigFileStore(properties)
        );
        assertTrue(passwordEncoder.matches("TestPassword#123", users.findByUsername("player").getPasswordHash()));
        assertTrue(passwordEncoder.matches("TestPassword#123", users.findByUsername("player").getPasswordHash()));
        assertTrue(Files.readString(xmlFile).contains("<!DOCTYPE users SYSTEM \"users.dtd\">"));
    }

    private AuthProperties properties(Path userFile) {
        return properties(userFile, null);
    }

    private AuthProperties properties(Path userFile, String initialPassword) {
        return new AuthProperties(
                userFile,
                tempDir.resolve("refresh-tokens.xml"),
                "01234567890123456789012345678901",
                900,
                3600,
                7200,
                false,
                initialPassword
        );
    }
}
