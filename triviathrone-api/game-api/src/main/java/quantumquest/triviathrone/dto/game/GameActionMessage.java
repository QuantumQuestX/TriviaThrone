package quantumquest.triviathrone.dto.game;

import java.util.Objects;
import java.util.Map;

public record GameActionMessage(String lobbyCode, String action, Map<String, Object> parameters) {
    public GameActionMessage {
        Objects.requireNonNull(lobbyCode, "lobbyCode");
        Objects.requireNonNull(action, "action");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
