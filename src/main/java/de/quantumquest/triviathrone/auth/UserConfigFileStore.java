package de.quantumquest.triviathrone.auth;

import de.quantumquest.triviathrone.dto.UserFile;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class UserConfigFileStore {
    private final Path file;

    public UserConfigFileStore(AuthProperties properties) {
        this.file = Objects.requireNonNull(properties, "properties")
                .userFile()
                .toAbsolutePath();
    }

    UserFile load() {
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("Users file not found: " + file);
        }
        return loadXml();
    }

    void save(UserFile data) {
        Objects.requireNonNull(data, "data");
        Path parent = file.getParent();
        Path temporaryFile = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            writeXml(temporaryFile, data);
            ValidatedXmlParser.parse(temporaryFile, "users.dtd", "users.xsd");
            Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | XMLStreamException | SAXException | ParserConfigurationException exception) {
            throw new IllegalStateException("Failed to persist users file to " + file, exception);
        }
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
            throw new IllegalStateException("Failed to load or validate users file " + file, exception);
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

    private static void writeXml(Path path, UserFile data) throws IOException, XMLStreamException {
        try (OutputStream output = Files.newOutputStream(path)) {
            XMLStreamWriter writer = XMLOutputFactory.newFactory()
                    .createXMLStreamWriter(output, StandardCharsets.UTF_8.name());
            try {
                writer.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
                writer.writeDTD("<!DOCTYPE users SYSTEM \"users.dtd\">");
                writer.writeStartElement("users");
                writer.writeNamespace("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
                writer.writeAttribute(
                        "xsi",
                        XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
                        "noNamespaceSchemaLocation",
                        "users.xsd"
                );

                for (UserAccount user : data.getUsers()) {
                    writeUser(writer, user);
                }

                writer.writeEndElement();
                writer.writeEndDocument();
                writer.flush();
            } finally {
                writer.close();
            }
        }
    }

    private static void writeUser(XMLStreamWriter writer, UserAccount user) throws XMLStreamException {
        Objects.requireNonNull(user, "user");
        writer.writeStartElement("user");
        writeElement(writer, "username", Objects.requireNonNull(user.getUsername(), "username"));
        writeOptionalElement(writer, "displayName", user.getDisplayName());
        writeOptionalElement(writer, "passwordHash", user.getPasswordHash());

        writer.writeStartElement("roles");
        for (String role : user.getRoles().stream().sorted().toList()) {
            writeElement(writer, "role", role);
        }
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private static void writeOptionalElement(XMLStreamWriter writer, String name, String value)
            throws XMLStreamException {
        if (value != null) {
            writeElement(writer, name, value);
        }
    }

    private static void writeElement(XMLStreamWriter writer, String name, String value)
            throws XMLStreamException {
        writer.writeStartElement(name);
        writer.writeCharacters(value);
        writer.writeEndElement();
    }

}
