package quantumquest.triviathrone.auth;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class RefreshTokenSessionStore {
    private final Path sessionFile;

    public RefreshTokenSessionStore(AuthProperties properties) {
        this.sessionFile = Objects.requireNonNull(properties, "properties")
                .refreshTokenFile()
                .toAbsolutePath();
    }

    Map<String, RefreshTokenSession> load() {
        Map<String, RefreshTokenSession> sessions = new HashMap<>();
        try {
            if (Files.exists(sessionFile)) {
                loadXmlSessions(sessions);
            }
            return sessions;
        } catch (IOException | SAXException | ParserConfigurationException exception) {
            throw new IllegalStateException("Failed to load refresh-token sessions from " + sessionFile, exception);
        }
    }

    void save(Map<String, RefreshTokenSession> sessions) {
        Objects.requireNonNull(sessions, "sessions");
        List<PersistedSession> persistedSessions = new ArrayList<>();
        sessions.forEach((tokenDigest, session) -> persistedSessions.add(new PersistedSession(
                tokenDigest,
                session.username(),
                session.expiresAt().getEpochSecond(),
                session.absoluteExpiry().getEpochSecond()
        )));

        Path parent = sessionFile.getParent();
        Path temporaryFile = sessionFile.resolveSibling(sessionFile.getFileName() + ".tmp");
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            writeXml(temporaryFile, persistedSessions);
            ValidatedXmlParser.parse(temporaryFile, "refresh-tokens.dtd", "refresh-tokens.xsd");
            Files.move(temporaryFile, sessionFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | XMLStreamException | SAXException | ParserConfigurationException exception) {
            throw new IllegalStateException("Failed to persist refresh-token sessions to " + sessionFile, exception);
        }
    }

    private void loadXmlSessions(Map<String, RefreshTokenSession> sessions)
            throws IOException, SAXException, ParserConfigurationException {
        Document document = ValidatedXmlParser.parse(
                sessionFile,
                "refresh-tokens.dtd",
                "refresh-tokens.xsd"
        );
        NodeList sessionNodes = document.getDocumentElement().getElementsByTagName("session");
        for (int index = 0; index < sessionNodes.getLength(); index++) {
            Element session = (Element) sessionNodes.item(index);
            String tokenDigest = requiredAttribute(session, "tokenDigest");
            String username = requiredAttribute(session, "username");
            long expiresAt = Long.parseLong(requiredAttribute(session, "expiresAtEpochSecond"));
            long absoluteExpiry = Long.parseLong(requiredAttribute(session, "absoluteExpiryEpochSecond"));
            addIfActive(sessions, tokenDigest, username, expiresAt, absoluteExpiry);
        }
    }

    private static void addIfActive(Map<String, RefreshTokenSession> sessions, String tokenDigest, String username,
                                    long expiresAtEpochSecond, long absoluteExpiryEpochSecond) {
        Instant expiry = Instant.ofEpochSecond(expiresAtEpochSecond);
        Instant absoluteExpiry = Instant.ofEpochSecond(absoluteExpiryEpochSecond);
        Instant now = Instant.now();
        if (now.isBefore(expiry) && now.isBefore(absoluteExpiry)) {
            sessions.put(tokenDigest, new RefreshTokenSession(username, expiry, absoluteExpiry));
        }
    }

    private static void writeXml(Path file, List<PersistedSession> persistedSessions)
            throws IOException, XMLStreamException {
        try (OutputStream output = Files.newOutputStream(file)) {
            XMLStreamWriter writer = XMLOutputFactory.newFactory()
                    .createXMLStreamWriter(output, StandardCharsets.UTF_8.name());
            try {
                writer.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
                writer.writeDTD("<!DOCTYPE refreshSessions SYSTEM \"refresh-tokens.dtd\">");
                writer.writeStartElement("refreshSessions");
                writer.writeNamespace("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
                writer.writeAttribute(
                        "xsi",
                        XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
                        "noNamespaceSchemaLocation",
                        "refresh-tokens.xsd"
                );
                for (PersistedSession session : persistedSessions) {
                    writer.writeStartElement("session");
                    writer.writeAttribute("tokenDigest", session.tokenDigest());
                    writer.writeAttribute("username", session.username());
                    writer.writeAttribute("expiresAtEpochSecond", Long.toString(session.expiresAtEpochSecond()));
                    writer.writeAttribute("absoluteExpiryEpochSecond",
                            Long.toString(session.absoluteExpiryEpochSecond()));
                    writer.writeEndElement();
                }
                writer.writeEndElement();
                writer.writeEndDocument();
                writer.flush();
            } finally {
                writer.close();
            }
        }
    }

    private static String requiredAttribute(Element element, String name) {
        String value = element.getAttribute(name);
        if (value.isBlank()) {
            throw new IllegalStateException("Refresh-token XML session is missing attribute: " + name);
        }
        return value;
    }

    private record PersistedSession(
            String tokenDigest,
            String username,
            long expiresAtEpochSecond,
            long absoluteExpiryEpochSecond
    ) {
    }
}
