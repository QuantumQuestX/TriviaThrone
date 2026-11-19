package quantumquest.triviathrone.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import quantumquest.triviathrone.api.system.events.LobbyClosedEvent;
import quantumquest.triviathrone.api.system.events.LobbyCreatedEvent;
import quantumquest.triviathrone.auth.PasswordUtils;
import quantumquest.triviathrone.auth.UserAccount;
import quantumquest.triviathrone.mod.SystemModManager;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates, tracks, and closes active lobbies.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Service
public class LobbyService {
    /**
     * Logs events from this component.
     */
    private static final Logger logger = LoggerFactory.getLogger(LobbyService.class);

    /**
     * Holds the 6 used by this component.
     */
    private static final int CODE_LENGTH = 6;
    /**
     * Stores an internal helper required by this component.
     */
    private final Map<String, Lobby> lobbies = new ConcurrentHashMap<>();
    /**
     * Generates cryptographically secure random values.
     */
    private final SecureRandom random = new SecureRandom();
    /**
     * Holds the syste ods used by this component.
     */
    private final SystemModManager systemMods;
    /**
     * Encodes passwords before they are stored in lobby state.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a LobbyService.
     *
     * @param systemMods      the system-mod manager
     * @param passwordEncoder encoder for lobby passwords
     */
    public LobbyService(SystemModManager systemMods) {
        this(systemMods, new BCryptPasswordEncoder());
    }

    public LobbyService(SystemModManager systemMods, PasswordEncoder passwordEncoder) {
        this.systemMods = Objects.requireNonNull(systemMods, "systemMods");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
    }

    /**
     * Creates a lobby hosted by the authenticated user.
     *
     * @param name        the lobby name
     * @param hostAccount the host account
     * @return the created a lobby hosted by the authenticated user
     */
    public Lobby createLobby(String name, UserAccount hostAccount) {
        return createLobby(name, hostAccount, null);
    }

    public Lobby createLobby(String name, UserAccount hostAccount, String password) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(hostAccount, "hostAccount");

        String normalizedPassword = password == null || password.isBlank() ? null : password;
        if (normalizedPassword != null) {
            PasswordUtils.validate(normalizedPassword);
        }

        String hostUsername = Objects.requireNonNull(hostAccount.getUsername(), "host username");
        Lobby lobby;
        String code;

        do {
            code = generateCode();
            String passwordHash = normalizedPassword == null
                ? null
                : passwordEncoder.encode(normalizedPassword);
            lobby = new Lobby(code, name, hostUsername, passwordHash);
        } while (lobbies.putIfAbsent(code, lobby) != null);

        logger.info("Creating lobby with code {} by {}", code, hostUsername);
        systemMods.publishEvent(new LobbyCreatedEvent(Instant.now(), code, hostUsername));
        return lobby;
    }

    /**
     * Removes a lobby from the active-lobby map.
     *
     * @param code the lobby code
     */
    public void removeLobby(String code) {
        Lobby removed = lobbies.remove(Objects.requireNonNull(code, "code"));
        if (removed != null) {
            systemMods.publishEvent(new LobbyClosedEvent(Instant.now(), removed.getLobbyCode()));
        }
    }

    /**
     * Finds an active lobby by its code.
     *
     * @param code lobby code to look up
     * @return the matching lobby, if it is active
     */
    public Optional<Lobby> getLobby(String code) {
        return Optional.ofNullable(lobbies.get(Objects.requireNonNull(code, "code")));
    }

    /**
     * Returns the currently active lobbies.
     *
     * @return the currently active lobbies
     */
    public List<Lobby> listLobbies() {
        return List.copyOf(lobbies.values());
    }

    /**
     * Generates a unique lobby code.
     */
    private String generateCode() {
        final String codeChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(codeChars.charAt(random.nextInt(codeChars.length())));
        }
        return sb.toString();
    }
}
