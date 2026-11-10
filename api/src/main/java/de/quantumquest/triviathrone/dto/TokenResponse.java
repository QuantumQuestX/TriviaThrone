package de.quantumquest.triviathrone.dto;

public record TokenResponse(String token, long expiresIn, String userId) {
}
