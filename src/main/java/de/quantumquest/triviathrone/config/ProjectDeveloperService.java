package de.quantumquest.triviathrone.config;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectDeveloperService {
    private static final String GENERATED_METADATA = "/project-metadata/developers.xml";

    private final List<Developer> developers;

    public ProjectDeveloperService() {
        this.developers = loadDevelopers();
    }

    public List<Developer> getDevelopers() {
        return developers;
    }

    private static List<Developer> loadDevelopers() {
        InputStream metadata = ProjectDeveloperService.class.getResourceAsStream(GENERATED_METADATA);
        if (metadata != null) {
            try (metadata) {
                return parseGeneratedMetadata(metadata);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not read generated developer metadata.", exception);
            }
        }

        Path projectPom = Path.of("pom.xml");
        if (Files.isRegularFile(projectPom)) {
            try (InputStream pom = Files.newInputStream(projectPom)) {
                return parsePom(pom);
            } catch (IOException | ParserConfigurationException | SAXException exception) {
                throw new IllegalStateException("Could not read developer metadata from the Maven POM.", exception);
            }
        }
        throw new IllegalStateException("Generated developer metadata is missing and the Maven POM is unavailable.");
    }

    private static List<Developer> parsePom(InputStream pom)
            throws IOException, ParserConfigurationException, SAXException {
        Document document = newDocumentBuilderFactory().newDocumentBuilder().parse(pom);
        NodeList developerNodes = document.getElementsByTagName("developer");
        List<Developer> developers = new ArrayList<>(developerNodes.getLength());
        for (int index = 0; index < developerNodes.getLength(); index++) {
            Element developer = (Element) developerNodes.item(index);
            String name = childText(developer, "name");
            if (name.isBlank()) {
                name = childText(developer, "id");
            }
            if (!name.isBlank()) {
                developers.add(new Developer(
                        name,
                        childText(developer, "url"),
                        childTexts(developer, "role"),
                        organizationsFromPom(developer)
                ));
            }
        }
        return List.copyOf(developers);
    }

    private static List<Developer> parseGeneratedMetadata(InputStream metadata) {
        try {
            Document document = newDocumentBuilderFactory().newDocumentBuilder().parse(metadata);
            NodeList developerNodes = document.getElementsByTagName("developer");
            List<Developer> developers = new ArrayList<>(developerNodes.getLength());
            for (int index = 0; index < developerNodes.getLength(); index++) {
                Element developer = (Element) developerNodes.item(index);
                String name = childText(developer, "name");
                if (!name.isBlank()) {
                    developers.add(new Developer(
                            name,
                            childText(developer, "profileUrl"),
                            childTexts(developer, "role"),
                            organizationsFromGeneratedMetadata(developer)
                    ));
                }
            }
            return List.copyOf(developers);
        } catch (IOException | ParserConfigurationException | SAXException exception) {
            throw new IllegalStateException("Could not read generated developer metadata.", exception);
        }
    }

    private static DocumentBuilderFactory newDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private static List<Organization> organizationsFromPom(Element developer) {
        List<String> names = childTexts(developer, "organization");
        List<String> urls = childTexts(developer, "organizationUrl");
        int count = Math.max(names.size(), urls.size());
        List<Organization> organizations = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            String url = index < urls.size() ? urls.get(index) : "";
            String name = index < names.size() ? names.get(index) : url;
            organizations.add(new Organization(name, url));
        }
        return List.copyOf(organizations);
    }

    private static List<Organization> organizationsFromGeneratedMetadata(Element developer) {
        NodeList organizations = developer.getElementsByTagName("organization");
        List<Organization> result = new ArrayList<>(organizations.getLength());
        for (int index = 0; index < organizations.getLength(); index++) {
            Element organization = (Element) organizations.item(index);
            result.add(new Organization(childText(organization, "name"), childText(organization, "url")));
        }
        return List.copyOf(result);
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

    public record Developer(String name, String url, List<String> roles, List<Organization> organizations) {
    }

    public record Organization(String name, String url) {
    }
}
