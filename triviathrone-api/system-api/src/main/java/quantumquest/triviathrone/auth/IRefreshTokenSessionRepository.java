package quantumquest.triviathrone.auth;

import java.util.Map;

public interface IRefreshTokenSessionRepository {
    Map<String, RefreshTokenSession> load();

    void save(Map<String, RefreshTokenSession> sessions);
}
