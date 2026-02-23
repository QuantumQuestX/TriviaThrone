package de.quantumquest.triviathrone.game;

import de.quantumquest.triviathrone.game.definition.Category;
import de.quantumquest.triviathrone.game.definition.QuestionDefinition;
import de.quantumquest.triviathrone.game.definition.Round;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GameDefinitionLoader {
    private GameDefinitionLoader() {}

    public static GameDefinition parse(Path lobbyDir) throws Exception {
        Path manifestXml = lobbyDir.resolve("manifest.xml");

        if (!Files.exists(manifestXml)) {
            throw new IllegalStateException("manifest.xml not found in lobby directory");
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(manifestXml.toFile());

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

            QuestionDefinition[][] matrix = new QuestionDefinition[categoryCount][questionCount];

            Element questionsElement = getSingleChild(roundElement, "questions");
            NodeList questionNodes = questionsElement.getElementsByTagName("question");

            for (int q = 0; i < questionNodes.getLength(); q++) {
                Element questionElement = (Element) questionNodes.item(q);

                int row = Integer.parseInt(questionElement.getAttribute("row"));
                int category = Integer.parseInt(questionElement.getAttribute("category"));
                int points = Integer.parseInt(questionElement.getAttribute("points"));

                String prompt = getTextContent(questionElement, "prompt");

                String mediaPath = null;
                if (questionElement.hasAttribute("media")) {
                    mediaPath = questionElement.getAttribute("media");
                }

                matrix[category][row] = new QuestionDefinition(points, prompt, mediaPath);
            }

            List<Category> categories = new ArrayList<>();

            for (int c = 0; c < categoryCount; c++) {
                List<QuestionDefinition> questions = new ArrayList<>();

                for (int r = 0; r < questionCount; r++) {
                    QuestionDefinition qDef = matrix[c][r];
                    if (qDef == null)
                        throw new IllegalStateException("missing question at category " + c + ", row " + r);
                    questions.add(qDef);
                }

                categories.add(new Category("Category " + c, questions));
            }

            rounds.add(new Round(roundName, categories));
        }

        return new GameDefinition(title, description, rounds);
    }

    private static Element getSingleChild(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        if (list.getLength() == 0) {
            throw new IllegalStateException("Missing element: "  + tag);
        }
        return (Element) list.item(0);
    }

    private static String getTextContent(Element root, String tag) {
        Element child = getSingleChild(root, tag);
        return child.getTextContent().trim();
    }

    private String getOptionalTextContent(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        if (list.getLength() == 0) return "";
        return list.item(0).getTextContent().trim();
    }
}
