package quantumquest.triviathrone.api.game;

import java.util.Objects;

public record GameCreatorInfo(
        String gameType,
        GameCreatorDescriptor descriptor
) {
    public GameCreatorInfo {
        Objects.requireNonNull(gameType, "gameType");
        Objects.requireNonNull(descriptor, "descriptor");
    }
}
