package de.quantumquest.triviathrone.games.jeopardy;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.quantumquest.triviathrone.api.game.GameCreationRequest;
import de.quantumquest.triviathrone.api.game.GameFileWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JeopardyGameCreatorTest {
    @TempDir
    Path tempDir;

    @Test
    void createsGameThatCanBeLoadedByItsProvider() throws IOException {
        ObjectNode data = JsonNodeFactory.instance.objectNode();
        data.put("title", "Test game");
        data.put("description", "A generated game");
        data.put("category", "Science");
        data.put("prompt", "What is H2O?");
        data.put("points", 200);
        data.put("choiceA", "Water");
        data.put("choiceB", "Oxygen");
        data.put("choiceC", "Hydrogen");
        data.put("choiceD", "Carbon");
        data.put("correctChoice", 0);

        GameFileWriter writer = relativePath -> {
            Path file = tempDir.resolve(relativePath);
            Files.createDirectories(file.getParent());
            return Files.newOutputStream(file);
        };
        new JeopardyGameCreator().create(new GameCreationRequest(data), writer);

        JeopardyDefinition definition = (JeopardyDefinition) new JeopardyGameProvider()
                .load(tempDir)
                .orElseThrow();
        assertEquals("Test game", definition.title());
        assertEquals("Science", definition.rounds().get(0).categories().get(0).name());
        assertEquals("What is H2O?", definition.rounds().get(0).categories().get(0).questions().get(0).prompt());
        assertEquals(0, definition.rounds().get(0).categories().get(0).questions().get(0).correctChoiceIndex());
    }

    @Test
    void packagesEditorAndGamePagesAsPluginResources() {
        ClassLoader pluginClassLoader = JeopardyGameProvider.class.getClassLoader();

        org.junit.jupiter.api.Assertions.assertNotNull(pluginClassLoader.getResource("ui/editor.html"));
        org.junit.jupiter.api.Assertions.assertNotNull(pluginClassLoader.getResource("ui/game.html"));
        org.junit.jupiter.api.Assertions.assertNotNull(pluginClassLoader.getResource("ui/css/game.css"));
        org.junit.jupiter.api.Assertions.assertNotNull(pluginClassLoader.getResource("ui/js/game/index.js"));
    }
}
