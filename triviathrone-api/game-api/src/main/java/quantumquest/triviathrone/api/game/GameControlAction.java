package quantumquest.triviathrone.api.game;

import java.util.Map;
import java.util.Optional;

public enum GameControlAction {
    START_GAME("START_GAME"),
    END_GAME("END_GAME"),
    SYNC("SYNC");

    private static final Map<String, GameControlAction> BY_WIRE_NAME = Map.of(
            START_GAME.wireName, START_GAME,
            END_GAME.wireName, END_GAME,
            SYNC.wireName, SYNC
    );

    private final String wireName;

    GameControlAction(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static Optional<GameControlAction> fromWireName(String wireName) {
        if (wireName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_WIRE_NAME.get(wireName));
    }
}
