package quantumquest.triviathrone.auth;

import java.util.Map;

/**
 * Persists refresh-token sessions keyed by token identifier.
 *
 * @author TiJ
 * @since 2.0.0
 */
public interface IRefreshTokenSessionRepository {
    /**
     * Loads the complete token-to-session mapping.
     */
    Map<String, RefreshTokenSession> load();

    /**
     * Replaces persisted sessions with the supplied token-to-session mapping.
     */
    void save(Map<String, RefreshTokenSession> sessions);
}
