package de.quantumquest.triviathrone.dto.game;

import java.util.List;

public record GameEndedMessage(
        String lobbyCode,
        boolean gameEnded,
        List<LeaderboardEntry> leaderboard
) {
    public GameEndedMessage {
        leaderboard = List.copyOf(leaderboard);
    }

    public record LeaderboardEntry(String username, int score) {
    }
}
