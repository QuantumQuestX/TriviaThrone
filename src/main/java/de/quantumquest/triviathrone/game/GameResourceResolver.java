package de.quantumquest.triviathrone.game;

import de.quantumquest.triviathrone.game.definition.MediaType;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class GameResourceResolver {
    private final Path lobbyDirectory;
    private final Map<String, String> resourcePaths;

    GameResourceResolver(Path lobbyDirectory, Element gameElement) {
        this.lobbyDirectory = Objects.requireNonNull(lobbyDirectory, "lobbyDirectory");
        this.resourcePaths = readResourcePaths(Objects.requireNonNull(gameElement, "gameElement"));
    }

    String mediaPathFor(Element question) {
        if (question.hasAttribute("media")) {
            return question.getAttribute("media");
        }

        NodeList mediaNodes = question.getElementsByTagName("media");
        if (mediaNodes.getLength() == 0) {
            return null;
        }

        String resourceId = ((Element) mediaNodes.item(0)).getAttribute("ref");
        String mediaPath = resourcePaths.get(resourceId);
        if (mediaPath == null) {
            throw new IllegalArgumentException("Question references an unknown media resource");
        }
        return mediaPath;
    }

    MediaType detectMediaType(String mediaPath) throws IOException {
        if (mediaPath == null || mediaPath.isBlank()) {
            return MediaType.UNDEFINED;
        }

        Path root = lobbyDirectory.toAbsolutePath().normalize();
        Path mediaFile = root.resolve(mediaPath).normalize();
        if (!mediaFile.startsWith(root)) {
            return MediaType.UNDEFINED;
        }
        return MediaType.detect(mediaFile);
    }

    private static Map<String, String> readResourcePaths(Element gameElement) {
        Map<String, String> paths = new HashMap<>();
        NodeList resourceNodes = gameElement.getElementsByTagName("resource");
        for (int index = 0; index < resourceNodes.getLength(); index++) {
            Element resource = (Element) resourceNodes.item(index);
            String id = resource.getAttribute("id");
            String file = resource.getAttribute("file");
            if (!id.isBlank() && !file.isBlank() && paths.putIfAbsent(id, file) != null) {
                throw new IllegalArgumentException("Duplicate resource id: " + id);
            }
        }
        return Map.copyOf(paths);
    }
}
