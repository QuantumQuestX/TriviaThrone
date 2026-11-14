package quantumquest.triviathrone.config;

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

/**
 * Loads project developer metadata for the public project-information API.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Service
public class ProjectDeveloperService {
    /**
     * Stores an internal helper required by this component.
     */
    private static final String GENERATED_METADATA = "/project-metadata/developers.xml";

    /**
     * Holds the developers used by this component.
     */
    private final List<Developer> developers;

    /**
     * Creates a ProjectDeveloperService.
     */
    public ProjectDeveloperService() {
        this.developers = loadDevelopers();
    }

    /**
     * Loads developer metadata bundled with the application.
     */
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

    /**
     * Parses developer information from the Maven POM.
     *
     * @param pom the POM XML stream
     * @return the declared project developers
     * @throws IOException                  if the stream cannot be read
     * @throws ParserConfigurationException if the XML parser cannot be configured
     * @throws SAXException                 if the POM XML is malformed
     */
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

    /**
     * Parses developers from generated XML metadata.
     */
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

    /**
     * Creates a hardened XML document-builder factory.
     */
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

    /**
     * Reads developer organizations from Maven POM metadata.
     */
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

    /**
     * Reads developer organizations from generated metadata.
     */
    private static List<Organization> organizationsFromGeneratedMetadata(Element developer) {
        NodeList organizations = developer.getElementsByTagName("organization");
        List<Organization> result = new ArrayList<>(organizations.getLength());
        for (int index = 0; index < organizations.getLength(); index++) {
            Element organization = (Element) organizations.item(index);
            result.add(new Organization(childText(organization, "name"), childText(organization, "url")));
        }
        return List.copyOf(result);
    }

    /**
     * Returns the text of a named child XML element.
     */
    private static String childText(Element parent, String tagName) {
        List<String> values = childTexts(parent, tagName);
        return values.isEmpty() ? "" : values.getFirst();
    }

    /**
     * Returns the text of all matching child XML elements.
     */
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

    /**
     * Returns the project developers loaded from build metadata.
     */
    public List<Developer> getDevelopers() {
        return developers;
    }

    /**
     * Describes a project developer.
     *
     * @param name          the developer name
     * @param url           the developer URL
     * @param roles         the developer roles
     * @param organizations the developer organizations
     * @author TiJ
     * @since 2.0.0
     */
    public record Developer(String name, String url, List<String> roles, List<Organization> organizations) {
    }

    /**
     * Describes a developer organization.
     *
     * @param name the organization name
     * @param url  the organization URL
     * @author TiJ
     * @since 2.0.0
     */
    public record Organization(String name, String url) {
    }
}
