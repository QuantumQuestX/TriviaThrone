package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.api.game.GamePackageManifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GamePackageManifestTest {
    @TempDir
    Path tempDir;

    @Test
    void parsesIndependentVersionsAndValidatesAssetHashes() throws IOException, NoSuchAlgorithmException {
        Files.writeString(tempDir.resolve("game.xml"), "<game version=\"format-7\"/>");
        byte[] assetContents = "bundled asset".getBytes(StandardCharsets.UTF_8);
        Files.write(tempDir.resolve("cover.png"), assetContents);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(assetContents));

        GamePackageManifest manifest = new GamePackageManifest(
                GamePackageManifest.CURRENT_MANIFEST_VERSION,
                "sample",
                "3.2.1",
                "2.0.0",
                "sample",
                "example.SampleGameProvider",
                "game.xml",
                "format-7",
                "2.0.0",
                new GamePackageManifest.PackageMetadata("Sample", "Author", "en"),
                List.of(new GamePackageManifest.Asset("cover.png", hash))
        );
        manifest.write(tempDir);

        GamePackageManifest parsed = GamePackageManifest.read(tempDir);

        assertEquals("1.0", parsed.manifestVersion());
        assertEquals("format-7", parsed.gameDataFormatVersion());
        assertEquals("3.2.1", parsed.pluginVersion());
        assertEquals("Sample", parsed.metadata().title());

        Files.writeString(tempDir.resolve("cover.png"), "tampered");
        assertThrows(IllegalArgumentException.class, () -> GamePackageManifest.read(tempDir));
    }

    @Test
    void rejectsPathsThatEscapeThePackage() {
        assertThrows(IllegalArgumentException.class, () -> new GamePackageManifest(
                GamePackageManifest.CURRENT_MANIFEST_VERSION,
                "sample",
                "1.0.0",
                "2.0.0",
                "sample",
                "example.SampleGameProvider",
                "../game.xml",
                "1.0",
                "2.0.0",
                null,
                List.of()
        ));
    }
}
