package de.quantumquest.triviathrone.games.jeopardy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import de.quantumquest.triviathrone.api.game.GameCreationRequest;
import de.quantumquest.triviathrone.api.game.GameCreatorDescriptor;
import de.quantumquest.triviathrone.api.game.GameFileWriter;
import de.quantumquest.triviathrone.api.game.IGameCreator;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public final class JeopardyGameCreator implements IGameCreator {
    private static final GameCreatorDescriptor DESCRIPTOR = new GameCreatorDescriptor(
            "Jeopardy",
            "Build a Jeopardy-style trivia board with custom rounds, categories, questions, and media.",
            editorSchema()
    );

    @Override
    public GameCreatorDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void create(GameCreationRequest request, GameFileWriter files) throws IOException {
        JsonNode data = request.data();
        String title = requiredText(data, "title");
        String description = requiredText(data, "description");
        String category = requiredText(data, "category");
        String prompt = requiredText(data, "prompt");
        String[] choices = {
                requiredText(data, "choiceA"),
                requiredText(data, "choiceB"),
                requiredText(data, "choiceC"),
                requiredText(data, "choiceD")
        };
        JsonNode pointsNode = data.get("points");
        JsonNode correctChoiceNode = data.get("correctChoice");
        if (pointsNode == null || !pointsNode.canConvertToInt()
                || correctChoiceNode == null || !correctChoiceNode.canConvertToInt()) {
            throw new IllegalArgumentException("Points and the correct choice must be integers.");
        }

        int points = pointsNode.intValue();
        int correctChoice = correctChoiceNode.intValue();
        if (points < 100 || points > 1000 || points % 100 != 0) {
            throw new IllegalArgumentException("Points must be between 100 and 1000 in increments of 100.");
        }
        if (correctChoice < 0 || correctChoice >= choices.length) {
            throw new IllegalArgumentException("The correct choice must be between 0 and 3.");
        }

        try (OutputStream output = files.createFile("manifest.xml")) {
            try {
                XMLStreamWriter xml = XMLOutputFactory.newFactory().createXMLStreamWriter(output, "UTF-8");
                xml.writeStartDocument("UTF-8", "1.0");
                xml.writeStartElement("game");
                xml.writeAttribute("version", "1.0");
                xml.writeAttribute("id", UUID.randomUUID().toString());
                writeTextElement(xml, "meta", "title", title, "description", description);
                xml.writeStartElement("rounds");
                xml.writeStartElement("round");
                xml.writeAttribute("name", "Round 1");
                xml.writeAttribute("categoryCount", "1");
                xml.writeAttribute("questionCount", "1");
                xml.writeStartElement("categories");
                xml.writeStartElement("category");
                xml.writeAttribute("row", "0");
                xml.writeCharacters(category);
                xml.writeEndElement();
                xml.writeEndElement();
                xml.writeStartElement("questions");
                xml.writeStartElement("question");
                xml.writeAttribute("row", "0");
                xml.writeAttribute("category", "0");
                xml.writeAttribute("points", Integer.toString(points));
                xml.writeAttribute("type", "mcq");
                xml.writeStartElement("prompt");
                xml.writeCharacters(prompt);
                xml.writeEndElement();
                xml.writeStartElement("options");
                for (int index = 0; index < choices.length; index++) {
                    xml.writeStartElement("option");
                    if (index == correctChoice) {
                        xml.writeAttribute("correct", "true");
                    }
                    xml.writeCharacters(choices[index]);
                    xml.writeEndElement();
                }
                xml.writeEndElement();
                xml.writeEndElement();
                xml.writeEndElement();
                xml.writeEndElement();
                xml.writeEndElement();
                xml.writeEndElement();
                xml.writeEndDocument();
                xml.flush();
            } catch (XMLStreamException exception) {
                throw new IOException("Could not write Jeopardy game manifest.", exception);
            }
        }
    }

    private static void writeTextElement(
            XMLStreamWriter xml,
            String parent,
            String firstName,
            String firstValue,
            String secondName,
            String secondValue
    ) throws XMLStreamException {
        xml.writeStartElement(parent);
        xml.writeStartElement(firstName);
        xml.writeCharacters(firstValue);
        xml.writeEndElement();
        xml.writeStartElement(secondName);
        xml.writeCharacters(secondValue);
        xml.writeEndElement();
        xml.writeEndElement();
    }

    private static String requiredText(JsonNode data, String field) {
        JsonNode value = data.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException("The field \"" + field + "\" is required.");
        }
        return value.textValue().trim();
    }

    private static JsonNode editorSchema() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        addStringField(properties, "title", "Game title");
        addStringField(properties, "description", "Description");
        addStringField(properties, "category", "Category");
        addStringField(properties, "prompt", "Question");

        ObjectNode points = properties.putObject("points");
        points.put("title", "Points");
        points.put("type", "integer");
        points.put("minimum", 100);
        points.put("maximum", 1000);
        points.put("multipleOf", 100);
        points.put("default", 100);

        addStringField(properties, "choiceA", "Choice A");
        addStringField(properties, "choiceB", "Choice B");
        addStringField(properties, "choiceC", "Choice C");
        addStringField(properties, "choiceD", "Choice D");

        ObjectNode correctChoice = properties.putObject("correctChoice");
        correctChoice.put("title", "Correct choice");
        correctChoice.put("type", "integer");
        ArrayNode choices = correctChoice.putArray("enum");
        choices.add(0).add(1).add(2).add(3);
        ArrayNode choiceNames = correctChoice.putArray("enumNames");
        choiceNames.add("Choice A").add("Choice B").add("Choice C").add("Choice D");
        correctChoice.put("default", 0);

        ArrayNode required = schema.putArray("required");
        required.add("title").add("description").add("category").add("prompt")
                .add("choiceA").add("choiceB").add("choiceC").add("choiceD").add("correctChoice");
        return schema;
    }

    private static void addStringField(ObjectNode properties, String name, String title) {
        ObjectNode property = properties.putObject(name);
        property.put("title", title);
        property.put("type", "string");
        property.put("minLength", 1);
    }
}
