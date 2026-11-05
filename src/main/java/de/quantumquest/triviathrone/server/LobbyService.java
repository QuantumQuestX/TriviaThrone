package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.auth.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LobbyService {
    private static final Logger logger = LoggerFactory.getLogger(LobbyService.class);

    private static final int CODE_LENGTH = 6;
    private final Map<String, Lobby> lobbies = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public Lobby createLobby(String name, UserAccount hostAccount) {
        Lobby lobby;
        String code;
        do {
            code = generateCode();
            lobby = new Lobby(code, name, hostAccount.getUsername());
        } while (lobbies.putIfAbsent(code, lobby) != null);

        logger.info("Creating lobby with code {} by {}", code, hostAccount.getUsername());
        return lobby;
    }

    public void removeLobby(String code) {
        lobbies.remove(code);
    }

    public Optional<Lobby> getLobby(String code) {
        return Optional.ofNullable(lobbies.get(code));
    }

    public List<Lobby> listLobbies() {
        return List.copyOf(lobbies.values());
    }

    public Optional<Lobby> getLobbyForUsername(String username) {
        return lobbies.values().stream().filter(lobby -> lobby.listUsernames().contains(username)).findFirst();
    }

    private String generateCode() {
        final String codeChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(codeChars.charAt(random.nextInt(codeChars.length())));
        }
        return sb.toString();
    }
}
