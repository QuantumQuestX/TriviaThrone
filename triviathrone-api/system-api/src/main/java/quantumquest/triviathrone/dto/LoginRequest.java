package quantumquest.triviathrone.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable credentials payload used to request authentication.
 *
 * @param username login name
 * @param password plaintext password supplied for authentication
 * @author TiJ
 * @since 2.0.0
 */
public record LoginRequest(
    /** JSON username property. */
        @JsonProperty("username") String username,
    /** JSON password property. */
        @JsonProperty("password") String password) {
    /**
     * Validates the credentials payload.
     *
     * @throws IllegalArgumentException if either credential is null or blank
     */
    public LoginRequest {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password must not be blank");
        }
    }
}
