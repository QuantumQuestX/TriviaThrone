package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.auth.UserAccount;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LobbyService {
    private final Map<String, Lobby> lobbies = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public Lobby createLobby(String name, UserAccount hostAccount) {
        String code;
        do {
            code = generateCode(6);
        } while (lobbies.containsKey(code));

        System.out.printf("[%s] create host = %s", getClass().getSimpleName(), hostAccount.getUsername());
        Lobby lobby = new Lobby(code, name, hostAccount.getUsername());
        lobbies.put(code, lobby);
        return lobby;
    }

    public void removeLobby(String code) {
        lobbies.remove(code);
    }
    
    public Optional<Lobby> getLobby(String code) {
        return Optional.ofNullable(lobbies.get(code));
    }
    
    public List<Lobby> listLobbies() {
        return new ArrayList<>(lobbies.values());
    }

    public Optional<Lobby> getLobbyForUsername(String username) {
        return lobbies.values().stream().filter(lobby -> lobby.listUsernames().contains(username)).findFirst();
    }
    
    private String generateCode(int length) {
        final String codeChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(codeChars.charAt(random.nextInt(codeChars.length())));
        }
        return sb.toString();
    }
}
