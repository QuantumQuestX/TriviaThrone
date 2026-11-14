package quantumquest.triviathrone.server.game.creation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import quantumquest.triviathrone.api.game.GameCreationAsset;
import quantumquest.triviathrone.api.game.GameCreationRequest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class GameCreationRequestParser {
    private final ObjectMapper mapper;

    GameCreationRequestParser(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    GameCreationRequest parse(Map<String, Object> requestBody) {
        Object data = requestBody.get("data");
        if (!(data instanceof Map<?, ?>)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game creation data must be a JSON object.");
        }

        JsonNode jsonData = mapper.valueToTree(data);
        return new GameCreationRequest(jsonData);
    }

    GameCreationRequest parseMultipart(
            String requestData,
            MultiValueMap<String, MultipartFile> uploadedFiles,
            Path uploadDirectory
    ) throws IOException {
        JsonNode jsonData;
        try {
            jsonData = mapper.readTree(requestData);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid game creation data.", exception);
        }
        if (jsonData == null || !jsonData.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game creation data must be a JSON object.");
        }

        Map<String, GameCreationAsset> assets = new HashMap<>();
        int assetIndex = 0;
        for (Map.Entry<String, List<MultipartFile>> entry : uploadedFiles.entrySet()) {
            String partName = entry.getKey();
            if (!partName.startsWith("asset_") || partName.length() == "asset_".length()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unexpected game asset upload.");
            }
            if (entry.getValue().size() != 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each game asset must be uploaded once.");
            }

            MultipartFile uploadedFile = entry.getValue().get(0);
            if (uploadedFile.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game assets cannot be empty.");
            }
            String resourceId = partName.substring("asset_".length());
            String filename = uploadedFile.getOriginalFilename();
            if (filename == null || filename.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game asset filenames are required.");
            }

            Path assetPath = uploadDirectory.resolve("asset-" + assetIndex++);
            uploadedFile.transferTo(assetPath);
            if (assets.putIfAbsent(resourceId, new GameCreationAsset(filename, assetPath)) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate game asset upload.");
            }
        }

        return new GameCreationRequest(jsonData, assets);
    }
}
