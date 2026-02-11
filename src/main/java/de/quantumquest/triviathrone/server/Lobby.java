package de.quantumquest.triviathrone.server;

import java.util.ArrayList;
import java.util.List;

public class Lobby {
    private final String code;
    private final String name;
    private final List<Player> players = new ArrayList<>();
    private boolean gameStarted = false;
    
    public Lobby(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public void addPlayer(Player player) {
        if (!players.contains(player))
            players.add(player);
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }

    public List<Player> getPlayers() {
        return List.copyOf(players);
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void startGame() {
        gameStarted = true;
    }

    @Override
    public String toString() {
        return String.format("Lobby[code=%s, name=%s, players=%s, started=%b]", code, name, players, gameStarted);
    }
}
