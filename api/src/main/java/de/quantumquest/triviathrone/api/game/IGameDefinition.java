package de.quantumquest.triviathrone.api.game;

import java.util.Set;

public interface IGameDefinition {
    String gameType();

    String title();

    boolean referencesMedia(String mediaPath);

    IGameSession createSession(String lobbyCode, Set<String> players);
}
