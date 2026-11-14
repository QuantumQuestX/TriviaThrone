package quantumquest.triviathrone.api.game;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Creates output files owned by a game-package creation operation.
 *
 * <p>Implementations define the destination package root. Callers must close each returned stream;
 * they must not retain the writer beyond the creation operation.</p>
 *
 * @author TiJ
 * @since 2.0.0
 */
public interface GameFileWriter {
    /**
     * Creates a writable file at a package-relative path.
     *
     * @param relativePath non-null path relative to the package root
     * @return a newly opened stream owned by the caller and requiring closure
     * @throws IOException if the file cannot be created
     */
    OutputStream createFile(String relativePath) throws IOException;
}
