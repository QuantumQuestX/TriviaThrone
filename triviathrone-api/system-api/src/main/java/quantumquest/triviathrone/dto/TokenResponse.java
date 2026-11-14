package quantumquest.triviathrone.dto;

/**
 * Immutable authentication-token response payload.
 *
 * @param token     issued token value
 * @param expiresIn token lifetime in seconds
 * @param userId    authenticated account identifier
 * @author TiJ
 * @since 2.0.0
 */
public record TokenResponse(String token, long expiresIn, String userId) {
}
