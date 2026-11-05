package de.quantumquest.triviathrone.game;

import de.quantumquest.triviathrone.game.definition.Category;
import de.quantumquest.triviathrone.game.definition.MediaType;
import de.quantumquest.triviathrone.game.definition.QuestionDefinition;
import de.quantumquest.triviathrone.game.definition.Round;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GameDefinitionLoader {
    private GameDefinitionLoader() {
    }

    public static GameDefinition parse(Path lobbyDir) {
        try {
            Path manifestXml = lobbyDir.resolve("manifest.xml");

            if (!Files.isRegularFile(manifestXml)) {
                throw new IllegalStateException("manifest.xml not found in lobby directory");
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Document doc = factory.newDocumentBuilder().parse(manifestXml.toFile());

            doc.getDocumentElement().normalize();

            Element gameElement = doc.getDocumentElement();

            Element metaElement = getSingleChild(gameElement, "meta");

            String title = getTextContent(metaElement, "title");
            String description = getTextContent(metaElement, "description");
            Map<String, String> resourcePaths = getResourcePaths(gameElement);

            Element roundsElement = getSingleChild(gameElement, "rounds");
            NodeList roundNodes = roundsElement.getElementsByTagName("round");

            List<Round> rounds = new ArrayList<>();

            for (int i = 0; i < roundNodes.getLength(); i++) {
                Element roundElement = (Element) roundNodes.item(i);

                String roundName = roundElement.getAttribute("name");

                int categoryCount = Integer.parseInt(roundElement.getAttribute("categoryCount"));
                int questionCount = Integer.parseInt(roundElement.getAttribute("questionCount"));
                if (categoryCount < 1 || questionCount < 1) {
                    throw new IllegalArgumentException("Each round must have categories and questions");
                }

                QuestionDefinition[][] matrix = new QuestionDefinition[categoryCount][questionCount];

                Element questionsElement = getSingleChild(roundElement, "questions");
                NodeList questionNodes = questionsElement.getElementsByTagName("question");

                for (int q = 0; q < questionNodes.getLength(); q++) {
                    Element questionElement = (Element) questionNodes.item(q);

                    int row = Integer.parseInt(questionElement.getAttribute("row"));
                    int category = Integer.parseInt(questionElement.getAttribute("category"));
                    int points = Integer.parseInt(questionElement.getAttribute("points"));
                    if (category < 0 || category >= categoryCount || row < 0 || row >= questionCount) {
                        throw new IllegalArgumentException("Question position is outside the configured board");
                    }
                    if (matrix[category][row] != null) {
                        throw new IllegalArgumentException("Duplicate question position");
                    }

                    String prompt = getTextContent(questionElement, "prompt");

                    String mediaPath = getMediaPath(questionElement, resourcePaths);
                    MediaType mediaType = detectMediaType(lobbyDir, mediaPath);

                    matrix[category][row] = new QuestionDefinition(points, prompt, mediaPath, mediaType);
                }

                List<Category> categories = new ArrayList<>();
                NodeList categoryNodes = getSingleChild(roundElement, "categories").getElementsByTagName("category");

                for (int c = 0; c < categoryCount; c++) {
                    List<QuestionDefinition> questions = new ArrayList<>();

                    for (int r = 0; r < questionCount; r++) {
                        QuestionDefinition qDef = matrix[c][r];
                        if (qDef == null)
                            throw new IllegalStateException("missing question at category " + c + ", row " + r);
                        questions.add(qDef);
                    }

                    String categoryName = c < categoryNodes.getLength()
                            ? categoryNodes.item(c).getTextContent().trim()
                            : "";
                    categories.add(new Category(
                            categoryName.isBlank() ? "Category " + (c + 1) : categoryName,
                            questions
                    ));
                }

                rounds.add(new Round(roundName, categories));
            }

            return new GameDefinition(title, description, rounds);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new IllegalStateException("Could not parse game manifest", e);
        }
    }

    private static Map<String, String> getResourcePaths(Element gameElement) {
        Map<String, String> resourcePaths = new HashMap<>();
        NodeList resourceNodes = gameElement.getElementsByTagName("resource");
        for (int i = 0; i < resourceNodes.getLength(); i++) {
            Element resource = (Element) resourceNodes.item(i);
            String id = resource.getAttribute("id");
            String file = resource.getAttribute("file");
            if (!id.isBlank() && !file.isBlank() && resourcePaths.putIfAbsent(id, file) != null) {
                throw new IllegalArgumentException("Duplicate resource id: " + id);
            }
        }
        return Map.copyOf(resourcePaths);
    }

    private static String getMediaPath(Element question, Map<String, String> resourcePaths) {
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

    private static MediaType detectMediaType(Path lobbyDir, String mediaPath) throws IOException {
        if (mediaPath == null || mediaPath.isBlank()) {
            return MediaType.UNDEFINED;
        }

        Path root = lobbyDir.toAbsolutePath().normalize();
        Path mediaFile = root.resolve(mediaPath).normalize();
        if (!mediaFile.startsWith(root)) {
            return MediaType.UNDEFINED;
        }
        return MediaType.detect(mediaFile);
    }

    private static Element getSingleChild(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        if (list.getLength() == 0) {
            throw new IllegalStateException("Missing element: " + tag);
        }
        return (Element) list.item(0);
    }

    private static String getTextContent(Element root, String tag) {
        Element child = getSingleChild(root, tag);
        return child.getTextContent().trim();
    }

}
