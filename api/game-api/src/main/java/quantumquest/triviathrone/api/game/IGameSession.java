package quantumquest.triviathrone.api.game;

import quantumquest.triviathrone.dto.game.GameActionMessage;

public interface IGameSession extends AutoCloseable {
    GameActionResult handleAction(GameActionMessage action, String username, boolean isHost);

    IGameMessage stateMessage();

    IGameMessage endedMessage(String hostUsername);

    void setStateChangeListener(Runnable listener);

    @Override
    void close();
}
