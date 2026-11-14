package quantumquest.triviathrone.auth;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public class RefreshTokenSessionStore {
    private final IRefreshTokenSessionRepository repository;

    public RefreshTokenSessionStore(IRefreshTokenSessionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    Map<String, RefreshTokenSession> load() {
        return repository.load();
    }

    void save(Map<String, RefreshTokenSession> sessions) {
        repository.save(sessions);
    }
}
