package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.auth.UserAccount;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LobbyService {
    private final Map<String, Lobby> lobbies = new HashMap<>();
    private final Random random = new Random();

    public Lobby createLobby(String name) {
        String code = generateCode();
        Lobby lobby = new Lobby(code, name);
        lobbies.put(code, lobby);
        return lobby;
    }

    public List<Lobby> listLobbies() {
        return new ArrayList<>(lobbies.values());
    }

    public Lobby joinLobby(String code, UserAccount user) {
        System.out.println(code);
        System.out.println(lobbies);
        Lobby lobby = lobbies.get(code);
        if (lobby == null) throw new IllegalArgumentException("Lobby not found.");
        lobby.addPlayer(new Player(user));
        return lobby;
    }

    public void leaveLobby(String code, UserAccount user) {
        Lobby lobby = lobbies.get(code);
        if (lobby != null) {
            lobby.getPlayers().removeIf(p -> p.getUserAccount() == user);
        }
    }

    private String generateCode() {
        char letter = (char) ('A' + random.nextInt(26));
        int number = 100 + random.nextInt(900);
        return letter + String.valueOf(number);
    }
}
