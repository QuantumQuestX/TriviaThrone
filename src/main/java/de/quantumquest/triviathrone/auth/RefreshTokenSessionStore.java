package de.quantumquest.triviathrone.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.InputStream;
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

@Component
public class RefreshTokenSessionStore {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path sessionFile;

    public RefreshTokenSessionStore(AuthProperties properties) {
        this.sessionFile = properties.refreshTokenFile().toAbsolutePath();
    }

    public Map<String, RefreshTokenSession> load() {
        Map<String, RefreshTokenSession> sessions = new HashMap<>();
        try {
            if (Files.exists(sessionFile)) {
                loadXmlSessions(sessions);
                return sessions;
            }

            Path legacyFile = legacyJsonFile();
            if (Files.exists(legacyFile)) {
                loadLegacyJsonSessions(legacyFile, sessions);
                save(sessions);
                Files.delete(legacyFile);
            }
            return sessions;
        } catch (IOException | XMLStreamException exception) {
            throw new IllegalStateException("Failed to load refresh-token sessions from " + sessionFile, exception);
        }
    }

    public void save(Map<String, RefreshTokenSession> sessions) {
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
            Files.move(temporaryFile, sessionFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | XMLStreamException exception) {
            throw new IllegalStateException("Failed to persist refresh-token sessions to " + sessionFile, exception);
        }
    }

    private void loadXmlSessions(Map<String, RefreshTokenSession> sessions)
            throws IOException, XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
            throw new XMLStreamException("External entities are disabled.");
        });

        boolean rootSeen = false;
        try (InputStream input = Files.newInputStream(sessionFile)) {
            XMLStreamReader reader = factory.createXMLStreamReader(input, StandardCharsets.UTF_8.name());
            try {
                while (reader.hasNext()) {
                    if (reader.next() != XMLStreamReader.START_ELEMENT) {
                        continue;
                    }

                    String element = reader.getLocalName();
                    if (!rootSeen && "refreshSessions".equals(element)) {
                        rootSeen = true;
                    } else if (rootSeen && "session".equals(element)) {
                        String tokenDigest = requiredAttribute(reader, "tokenDigest");
                        String username = requiredAttribute(reader, "username");
                        long expiresAt = Long.parseLong(requiredAttribute(reader, "expiresAtEpochSecond"));
                        long absoluteExpiry = Long.parseLong(requiredAttribute(reader, "absoluteExpiryEpochSecond"));
                        addIfActive(sessions, tokenDigest, username, expiresAt, absoluteExpiry);
                    } else {
                        throw new IllegalStateException("Refresh-token XML contains an unexpected element: " + element);
                    }
                }
            } finally {
                reader.close();
            }
        }

        if (!rootSeen) {
            throw new IllegalStateException("Refresh-token XML is empty or malformed: " + sessionFile);
        }
    }

    private void loadLegacyJsonSessions(Path legacyFile, Map<String, RefreshTokenSession> sessions)
            throws IOException {
        PersistedSessions persisted = objectMapper.readValue(legacyFile.toFile(), PersistedSessions.class);
        if (persisted == null || persisted.sessions() == null) {
            throw new IllegalStateException("Legacy refresh-token file is empty or malformed: " + legacyFile);
        }

        for (PersistedSession entry : persisted.sessions()) {
            if (entry == null || entry.tokenDigest() == null || entry.username() == null) {
                throw new IllegalStateException("Legacy refresh-token file contains an invalid session: " + legacyFile);
            }
            addIfActive(sessions, entry.tokenDigest(), entry.username(),
                    entry.expiresAtEpochSecond(), entry.absoluteExpiryEpochSecond());
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
                writer.writeStartElement("refreshSessions");
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

    private static String requiredAttribute(XMLStreamReader reader, String name) {
        String value = reader.getAttributeValue(null, name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Refresh-token XML session is missing attribute: " + name);
        }
        return value;
    }

    private Path legacyJsonFile() {
        String fileName = sessionFile.getFileName().toString();
        if (fileName.endsWith(".xml")) {
            return sessionFile.resolveSibling(fileName.substring(0, fileName.length() - 4) + ".json");
        }
        return sessionFile.resolveSibling("refresh-tokens.json");
    }

    private record PersistedSessions(List<PersistedSession> sessions) {
    }

    private record PersistedSession(
            String tokenDigest,
            String username,
            long expiresAtEpochSecond,
            long absoluteExpiryEpochSecond
    ) {
    }
}
