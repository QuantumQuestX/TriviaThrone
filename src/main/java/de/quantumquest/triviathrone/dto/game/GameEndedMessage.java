package de.quantumquest.triviathrone.dto.game;

import java.util.List;
import java.util.Objects;

public record GameEndedMessage(
        String lobbyCode,
        boolean gameEnded,
        List<LeaderboardEntry> leaderboard
) {
    public GameEndedMessage {
        Objects.requireNonNull(lobbyCode, "lobbyCode");
        leaderboard = List.copyOf(Objects.requireNonNull(leaderboard, "leaderboard"));
    }

    public record LeaderboardEntry(String username, int score) {
        public LeaderboardEntry {
            Objects.requireNonNull(username, "username");
        }
    }
}
