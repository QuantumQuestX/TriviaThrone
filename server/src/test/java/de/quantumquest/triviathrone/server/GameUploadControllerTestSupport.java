package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.auth.UserAccount;
import de.quantumquest.triviathrone.api.game.IGameDefinition;
import de.quantumquest.triviathrone.api.game.IGameProvider;
import de.quantumquest.triviathrone.api.game.IGameSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class GameUploadControllerTestSupport {
    static final String LOBBY_CODE = "ROOM01";

    private GameUploadControllerTestSupport() {
    }

    static Fixture createFixture(Path tempDir) {
        LobbyService lobbyService = mock(LobbyService.class);
        LobbyEventService lobbyEventService = mock(LobbyEventService.class);
        Path storageDir = tempDir.resolve("games");
        GameUploadController controller = new GameUploadController(
                lobbyService,
                lobbyEventService,
                new LobbyGameFiles(storageDir.toString(), new GameProviderRegistry(
                        java.util.List.of(uploadedGameProvider())
                ))
        );
        Lobby lobby = new Lobby(LOBBY_CODE, "Test lobby", "host");
        lobby.addUser("player");
        when(lobbyService.getLobby(LOBBY_CODE)).thenReturn(Optional.of(lobby));
        return new Fixture(lobbyEventService, controller, lobby, storageDir);
    }

    static IGameDefinition definition(String mediaPath) {
        return new IGameDefinition() {
            @Override
            public String gameType() {
                return "test";
            }

            @Override
            public String title() {
                return "Test game";
            }

            @Override
            public boolean referencesMedia(String requestedPath) {
                return mediaPath.equals(requestedPath);
            }

            @Override
            public IGameSession createSession(String lobbyCode, java.util.Set<String> players) {
                throw new UnsupportedOperationException("Test definition does not create sessions");
            }
        };
    }

    static UserAccount user(String username) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        return user;
    }

    private static IGameProvider uploadedGameProvider() {
        return new IGameProvider() {
            @Override
            public String gameType() {
                return "jeopardy";
            }

            @Override
            public boolean supportsFile(String filename) {
                return filename != null && filename.endsWith(".game.throne");
            }

            @Override
            public Optional<IGameDefinition> load(Path gameDirectory) throws IOException {
                if (!Files.isRegularFile(gameDirectory.resolve("manifest.xml"))
                        || !Files.isRegularFile(gameDirectory.resolve("game.xml"))) {
                    return Optional.empty();
                }
                return Optional.of(new IGameDefinition() {
                    @Override
                    public String gameType() {
                        return "jeopardy";
                    }

                    @Override
                    public String title() {
                        return "Uploaded game";
                    }

                    @Override
                    public boolean referencesMedia(String mediaPath) {
                        return "resources/clue.txt".equals(mediaPath);
                    }

                    @Override
                    public IGameSession createSession(String lobbyCode, java.util.Set<String> players) {
                        throw new UnsupportedOperationException("Test definition does not create sessions");
                    }
                });
            }
        };
    }

    static byte[] gameArchive() throws IOException {
        String gameData = """
                <game version="1.0">
                    <meta><title>Uploaded game</title><description>Test</description></meta>
                    <resources><resource id="clue" file="resources/clue.txt"/></resources>
                    <rounds>
                        <round name="Round 1" categoryCount="1" questionCount="1">
                            <categories><category>Category</category></categories>
                            <questions>
                                <question row="0" category="0" points="100">
                                    <prompt>Prompt</prompt>
                                    <media ref="clue"/>
                                </question>
                            </questions>
                        </round>
                    </rounds>
                </game>
                """;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream archive = new ZipOutputStream(bytes)) {
            String packageManifest = """
                    <gamePackage manifestVersion="2.0">
                        <mod id="jeopardy" version="test" apiVersion="test"/>
                        <game type="jeopardy" providerClass="%s" dataFile="game.xml" formatVersion="1.0"/>
                        <createdBy serverVersion="test"/>
                    </gamePackage>
                    """.formatted(uploadedGameProvider().getClass().getName());
            archive.putNextEntry(new ZipEntry("manifest.xml"));
            archive.write(packageManifest.getBytes(StandardCharsets.UTF_8));
            archive.closeEntry();
            archive.putNextEntry(new ZipEntry("game.xml"));
            archive.write(gameData.getBytes(StandardCharsets.UTF_8));
            archive.closeEntry();
            archive.putNextEntry(new ZipEntry("resources/clue.txt"));
            archive.write("clue data".getBytes(StandardCharsets.UTF_8));
            archive.closeEntry();
        }
        return bytes.toByteArray();
    }

    record Fixture(
            LobbyEventService lobbyEventService,
            GameUploadController controller,
            Lobby lobby,
            Path storageDir
    ) {
    }
}
