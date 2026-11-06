package de.quantumquest.triviathrone.auth;

import java.time.Instant;

record RefreshTokenSession(String username, Instant expiresAt, Instant absoluteExpiry) {
}
