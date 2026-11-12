package de.quantumquest.triviathrone.server;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.quantumquest.triviathrone.api.game.GameCreationRequest;
import de.quantumquest.triviathrone.api.game.GameCreatorDescriptor;
import de.quantumquest.triviathrone.api.game.GameCreatorInfo;
import de.quantumquest.triviathrone.auth.UserAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameCreatorControllerTest {
    @TempDir
    Path tempDir;

    @Test
    void serializesEditorSchemaAsJsonCompatibleWithTheApplicationMapper() throws Exception {
        GameCreationService service = mock(GameCreationService.class);
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        schema.putObject("properties").putObject("title").put("type", "string");
        when(service.creators()).thenReturn(List.of(new GameCreatorInfo(
                "jeopardy",
                new GameCreatorDescriptor("Jeopardy", "Create a game.", schema)
        )));
        when(service.hasCustomEditor("jeopardy")).thenReturn(true);

        JsonMapper mapper = JsonMapper.builder().build();
        JsonNode response = mapper.readTree(mapper.writeValueAsString(new GameCreatorController(service).creators()));

        JsonNode editorSchema = response.get(0).get("descriptor").get("editorSchema");
        assertEquals("object", editorSchema.get("type").stringValue());
        assertTrue(editorSchema.get("properties").has("title"));
        assertTrue(response.get(0).get("hasCustomEditor").booleanValue());
    }

    @Test
    void convertsMapRequestBodyIntoPluginJsonData() throws Exception {
        GameCreationService service = mock(GameCreationService.class);
        Path archive = Files.createFile(tempDir.resolve("generated.game.throne"));
        when(service.generate(eq("jeopardy"), any(GameCreationRequest.class)))
                .thenReturn(new GameCreationService.GeneratedGame(
                        tempDir,
                        archive,
                        "jeopardy.game.throne"
                ));
        GameCreatorController controller = new GameCreatorController(service);

        controller.create(
                "jeopardy",
                Map.of("data", Map.of("title", "Test game", "points", 200)),
                new UserAccount()
        );

        org.mockito.ArgumentCaptor<GameCreationRequest> request =
                org.mockito.ArgumentCaptor.forClass(GameCreationRequest.class);
        verify(service).generate(eq("jeopardy"), request.capture());
        assertEquals("Test game", request.getValue().data().get("title").textValue());
        assertEquals(200, request.getValue().data().get("points").intValue());
    }
}
