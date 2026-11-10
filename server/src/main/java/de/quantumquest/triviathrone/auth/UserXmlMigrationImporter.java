package de.quantumquest.triviathrone.auth;

import de.quantumquest.triviathrone.dto.UserFile;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class UserXmlMigrationImporter {
    private final Path file;

    public UserXmlMigrationImporter(AuthProperties properties) {
        this.file = Objects.requireNonNull(properties, "properties")
                .userMigrationFile()
                .toAbsolutePath();
    }

    UserFile load() {
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("User XML migration file not found: " + file);
        }
        return loadXml();
    }

    String fileName() {
        return file.getFileName().toString();
    }

    private UserFile loadXml() {
        try {
            Document document = ValidatedXmlParser.parse(file, "users.dtd", "users.xsd");
            NodeList userNodes = document.getDocumentElement().getElementsByTagName("user");
            List<UserAccount> users = new ArrayList<>(userNodes.getLength());
            for (int index = 0; index < userNodes.getLength(); index++) {
                Element userElement = (Element) userNodes.item(index);
                UserAccount user = new UserAccount();
                user.setUsername(requiredUsername(userElement));
                user.setDisplayName(optionalText(userElement, "displayName"));
                user.setPasswordHash(optionalText(userElement, "passwordHash"));
                user.setRoles(readRoles(userElement));
                users.add(user);
            }

            UserFile userFile = new UserFile();
            userFile.setUsers(users);
            return userFile;
        } catch (IOException | SAXException | ParserConfigurationException exception) {
            throw new IllegalStateException("Failed to load or validate user XML migration file " + file, exception);
        }
    }

    private static Set<String> readRoles(Element userElement) {
        Element rolesElement = optionalChild(userElement, "roles");
        if (rolesElement == null) {
            return Set.of();
        }

        NodeList roleNodes = rolesElement.getElementsByTagName("role");
        Set<String> roles = new HashSet<>();
        for (int index = 0; index < roleNodes.getLength(); index++) {
            roles.add(roleNodes.item(index).getTextContent());
        }
        return Set.copyOf(roles);
    }

    private static String requiredUsername(Element userElement) {
        Element child = optionalChild(userElement, "username");
        if (child == null) {
            throw new IllegalStateException("User XML is missing element: username");
        }
        return child.getTextContent();
    }

    private static String optionalText(Element parent, String name) {
        Element child = optionalChild(parent, name);
        return child == null ? null : child.getTextContent();
    }

    private static Element optionalChild(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element element && name.equals(element.getTagName())) {
                return element;
            }
        }
        return null;
    }

}
