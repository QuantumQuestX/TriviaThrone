package quantumquest.triviathrone.exceptions;

/**
 * Signals that a password does not satisfy the configured strength rules.
 *
 * @author TiJ
 * @since 2.0.0
 */
public class WeakPasswordException extends RuntimeException {
    /**
     * Creates a WeakPasswordException.
     *
     * @param message the STOMP message
     */
    public WeakPasswordException(String message) {
        super(message);
    }
}
