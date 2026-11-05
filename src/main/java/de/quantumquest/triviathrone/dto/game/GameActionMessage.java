package de.quantumquest.triviathrone.dto.game;

public record GameActionMessage(String lobbyCode, ActionType action,
                                Integer roundIdx, Integer categoryIdx, Integer questionIdx) {
    public enum ActionType {
        START_GAME,
        END_GAME,
        SYNC,
        SELECT_ROUND,
        SELECT_QUESTION,
        OPEN_BUZZ,
        CLOSE_BUZZ,
        BUZZ,
        MARK_CORRECT,
        MARK_INCORRECT,
        RESET
    }
}
