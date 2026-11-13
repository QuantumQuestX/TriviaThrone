package de.quantumquest.triviathrone.api.game;

import java.io.IOException;

public interface IGameCreator {
    GameCreatorDescriptor descriptor();

    void create(GameCreationRequest request, GameFileWriter files) throws IOException;
}
