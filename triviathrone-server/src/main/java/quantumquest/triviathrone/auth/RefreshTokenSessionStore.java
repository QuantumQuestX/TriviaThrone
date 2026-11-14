package quantumquest.triviathrone.auth;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * Adapts the configured repository for refresh-token session persistence.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Component
public class RefreshTokenSessionStore {
    /**
     * Holds the repository used by this component.
     */
    private final IRefreshTokenSessionRepository repository;

    /**
     * Creates a RefreshTokenSessionStore.
     *
     * @param repository the backing repository
     */
    public RefreshTokenSessionStore(IRefreshTokenSessionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * Loads the stored refresh-token sessions.
     *
     * @return the stored sessions keyed by token value
     */
    Map<String, RefreshTokenSession> load() {
        return repository.load();
    }

    /**
     * Saves the supplied refresh-token sessions.
     *
     * @param sessions the sessions to save, keyed by token value
     */
    void save(Map<String, RefreshTokenSession> sessions) {
        repository.save(sessions);
    }
}
