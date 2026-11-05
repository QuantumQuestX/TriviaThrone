package de.quantumquest.triviathrone.game;

import de.quantumquest.triviathrone.game.definition.Category;
import de.quantumquest.triviathrone.game.definition.QuestionDefinition;
import de.quantumquest.triviathrone.game.definition.Round;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GameDefinitionLoader {
    private GameDefinitionLoader() {
    }

    public static GameDefinition parse(Path lobbyDir) {
        try {
            Path manifestXml = lobbyDir.resolve("manifest.xml");

            if (!Files.exists(manifestXml)) {
                throw new IllegalStateException("manifest.xml not found in lobby directory");
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);

            Document doc = factory.newDocumentBuilder().parse(manifestXml.toFile());

            doc.getDocumentElement().normalize();

            Element gameElement = doc.getDocumentElement();

            Element metaElement = getSingleChild(gameElement, "meta");

            String title = getTextContent(metaElement, "title");
            String description = getTextContent(metaElement, "description");

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

                    String mediaPath = null;
                    if (questionElement.hasAttribute("media")) {
                        mediaPath = questionElement.getAttribute("media");
                    }

                    matrix[category][row] = new QuestionDefinition(points, prompt, mediaPath);
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
        } catch (java.io.IOException | javax.xml.parsers.ParserConfigurationException
                 | org.xml.sax.SAXException e) {
            throw new IllegalStateException("Could not parse game manifest", e);
        }
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
