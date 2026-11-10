import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GenerateProjectDevelopers {
    public static void main(String[] args) throws Exception {
        Path pomPath = Path.of(args[0]);
        Path outputPath = Path.of(args[1]);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        Document pom;
        try (InputStream input = Files.newInputStream(pomPath)) {
            pom = factory.newDocumentBuilder().parse(input);
        }

        Files.createDirectories(outputPath.getParent());
        try (var output = Files.newOutputStream(outputPath)) {
            XMLStreamWriter writer = XMLOutputFactory.newFactory().createXMLStreamWriter(output, "UTF-8");
            writeDevelopers(writer, pom.getElementsByTagName("developer"));
            writer.close();
        }
    }

    private static void writeDevelopers(XMLStreamWriter writer, NodeList developerNodes)
            throws XMLStreamException {
        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("developers");
        for (int index = 0; index < developerNodes.getLength(); index++) {
            Element developer = (Element) developerNodes.item(index);
            String name = childText(developer, "name");
            if (name.isBlank()) {
                name = childText(developer, "id");
            }
            if (name.isBlank()) {
                continue;
            }

            writer.writeStartElement("developer");
            writeTextElement(writer, "name", name);
            writeTextElement(writer, "profileUrl", childText(developer, "url"));
            writeTextCollection(writer, "roles", "role", childTexts(developer, "role"));
            writeOrganizations(writer, developer);
            writer.writeEndElement();
        }
        writer.writeEndElement();
        writer.writeEndDocument();
    }

    private static void writeOrganizations(XMLStreamWriter writer, Element developer) throws XMLStreamException {
        List<String> names = childTexts(developer, "organization");
        List<String> urls = childTexts(developer, "organizationUrl");
        int count = Math.max(names.size(), urls.size());
        writer.writeStartElement("organizations");
        for (int index = 0; index < count; index++) {
            String url = index < urls.size() ? urls.get(index) : "";
            String name = index < names.size() ? names.get(index) : url;
            writer.writeStartElement("organization");
            writeTextElement(writer, "name", name);
            writeTextElement(writer, "url", url);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private static void writeTextCollection(
            XMLStreamWriter writer,
            String wrapper,
            String item,
            List<String> values
    ) throws XMLStreamException {
        writer.writeStartElement(wrapper);
        for (String value : values) {
            writeTextElement(writer, item, value);
        }
        writer.writeEndElement();
    }

    private static void writeTextElement(XMLStreamWriter writer, String element, String value)
            throws XMLStreamException {
        writer.writeStartElement(element);
        writer.writeCharacters(value);
        writer.writeEndElement();
    }

    private static String childText(Element parent, String tagName) {
        List<String> values = childTexts(parent, tagName);
        return values.isEmpty() ? "" : values.getFirst();
    }

    private static List<String> childTexts(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        List<String> values = new ArrayList<>(nodes.getLength());
        for (int index = 0; index < nodes.getLength(); index++) {
            String value = nodes.item(index).getTextContent().trim();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }
}
