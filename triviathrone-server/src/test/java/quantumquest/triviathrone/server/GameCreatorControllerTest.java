package quantumquest.triviathrone.server;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import quantumquest.triviathrone.api.game.GameCreationAsset;
import quantumquest.triviathrone.api.game.GameCreationRequest;
import quantumquest.triviathrone.api.game.GameCreatorDescriptor;
import quantumquest.triviathrone.api.game.GameCreatorInfo;
import quantumquest.triviathrone.auth.UserAccount;
import quantumquest.triviathrone.server.game.creation.GameCreationService;
import quantumquest.triviathrone.server.game.creation.GameCreatorController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void convertsMapRequestBodyIntoGameCreatorJsonData() throws Exception {
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

    @Test
    void receivesMultipartEditorDataAndTemporarilyStoresUploadedAssets() throws Exception {
        GameCreationService service = mock(GameCreationService.class);
        Path archive = Files.createFile(tempDir.resolve("generated.game.throne"));
        when(service.generate(eq("jeopardy"), any(GameCreationRequest.class)))
                .thenReturn(new GameCreationService.GeneratedGame(tempDir, archive, "jeopardy.game.throne"));

        ObjectNode data = JsonNodeFactory.instance.objectNode();
        data.putObject("meta").put("title", "Test game");
        data.putArray("resources").addObject().put("id", "resource-id");
        MultiValueMap<String, MultipartFile> files = new LinkedMultiValueMap<>();
        files.add("asset_resource-id", new MockMultipartFile(
                "asset_resource-id",
                "cover.png",
                "image/png",
                new byte[]{1, 2, 3}
        ));

        new GameCreatorController(service).createMultipart(
                "jeopardy",
                new ObjectMapper().writeValueAsString(data),
                files,
                new UserAccount()
        );

        org.mockito.ArgumentCaptor<GameCreationRequest> request =
                org.mockito.ArgumentCaptor.forClass(GameCreationRequest.class);
        verify(service).generate(eq("jeopardy"), request.capture());
        GameCreationAsset asset = request.getValue().assets().get("resource-id");
        assertEquals("cover.png", asset.filename());
        assertFalse(Files.exists(asset.path()));
    }
}
