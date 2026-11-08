package de.quantumquest.triviathrone.dto.game;

import java.util.Objects;

public record GameActionMessage(String lobbyCode, ActionType action,
                                Integer roundIdx, Integer categoryIdx, Integer questionIdx) {
    public GameActionMessage {
        Objects.requireNonNull(lobbyCode, "lobbyCode");
        Objects.requireNonNull(action, "action");
    }

    public enum ActionType {
        START_GAME,
        END_GAME,
        SYNC,
        SELECT_ROUND,
        SELECT_QUESTION,
        PLAY_MEDIA,
        OPEN_BUZZ,
        CLOSE_BUZZ,
        BUZZ,
        MARK_CORRECT,
        MARK_INCORRECT,
        RESET
    }
}
