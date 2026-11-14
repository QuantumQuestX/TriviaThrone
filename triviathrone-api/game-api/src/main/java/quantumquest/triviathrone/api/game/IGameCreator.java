package quantumquest.triviathrone.api.game;

import java.io.IOException;

/**
 * Creates a game package from editor-supplied data.
 *
 * <p>Implementations are invoked for one creation operation at a time and must close every stream
 * obtained from the supplied writer.</p>
 *
 * @author TiJ
 * @since 2.0.0
 */
public interface IGameCreator {
    /**
     * Describes this creator for editor clients.
     *
     * @return a non-null immutable descriptor
     */
    GameCreatorDescriptor descriptor();

    /**
     * Writes a game package using the supplied request.
     *
     * @param request non-null immutable request snapshot
     * @param files   non-null writer scoped to this creation operation
     * @throws IOException if package output cannot be written
     */
    void create(GameCreationRequest request, GameFileWriter files) throws IOException;
}
