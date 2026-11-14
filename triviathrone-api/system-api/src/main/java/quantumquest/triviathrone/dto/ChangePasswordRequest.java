package quantumquest.triviathrone.dto;

/**
 * Immutable request payload for changing an authenticated user's password.
 *
 * @param oldPassword current password supplied for verification
 * @param newPassword replacement password
 * @author TiJ
 * @since 2.0.0
 */
public record ChangePasswordRequest(String oldPassword, String newPassword) {
}
