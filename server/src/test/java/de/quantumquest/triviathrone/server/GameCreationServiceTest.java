package de.quantumquest.triviathrone.server;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import de.quantumquest.triviathrone.api.game.GameCreationRequest;
import de.quantumquest.triviathrone.api.game.GameCreatorDescriptor;
import de.quantumquest.triviathrone.api.game.GameFileWriter;
import de.quantumquest.triviathrone.api.game.IGameCreator;
import de.quantumquest.triviathrone.api.game.IGameDefinition;
import de.quantumquest.triviathrone.api.game.IGameProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameCreationServiceTest {
    @Test
    void generatesLoadableGameArchiveAndCleansUpTemporaryFiles() throws Exception {
        IGameCreator creator = creatorWritingFile("manifest.xml", "<game/>");
        IGameProvider provider = providerWith(creator);
        GameCreationService service = new GameCreationService(new GameProviderRegistry(List.of(provider)));

        GameCreationService.GeneratedGame generated = service.generate(
                "sample",
                new GameCreationRequest(JsonNodeFactory.instance.objectNode())
        );
        Path temporaryDirectory = generated.temporaryDirectory();
        try (generated; ZipFile archive = new ZipFile(generated.archive().toFile())) {
            assertEquals("sample.game.throne", generated.filename());
            try (InputStream manifest = archive.getInputStream(archive.getEntry("manifest.xml"))) {
                assertEquals("<game/>", new String(manifest.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        assertFalse(Files.exists(temporaryDirectory));
    }

    @Test
    void rejectsPluginFilePathsThatEscapeTheGameDirectory() throws IOException {
        IGameCreator creator = creatorWritingFile("../outside.txt", "invalid");
        GameCreationService service = new GameCreationService(
                new GameProviderRegistry(List.of(providerWith(creator)))
        );

        assertThrows(IllegalArgumentException.class, () -> service.generate(
                "sample",
                new GameCreationRequest(JsonNodeFactory.instance.objectNode())
        ));
    }

    private static IGameCreator creatorWritingFile(String path, String content) {
        return new IGameCreator() {
            @Override
            public GameCreatorDescriptor descriptor() {
                return new GameCreatorDescriptor("Test", "", JsonNodeFactory.instance.objectNode());
            }

            @Override
            public void create(GameCreationRequest request, GameFileWriter writer) throws IOException {
                try (OutputStream output = writer.createFile(path)) {
                    output.write(content.getBytes(StandardCharsets.UTF_8));
                }
            }
        };
    }

    private static IGameProvider providerWith(IGameCreator creator) throws IOException {
        IGameProvider provider = mock(IGameProvider.class);
        when(provider.gameType()).thenReturn("sample");
        when(provider.creator()).thenReturn(Optional.of(creator));
        when(provider.load(any(Path.class))).thenReturn(Optional.of(mock(IGameDefinition.class)));
        return provider;
    }
}
