package de.quantumquest.triviathrone.dto;

import de.quantumquest.triviathrone.game.GameSession;

import java.util.Map;

public record GameStateMessage(String lobbyCode, GameSession.State state, String buzzWinner, Map<String, Integer> scores,
                               Integer curRoundIdx, Integer curCatIdx, Integer curQuestionIdx) {}
