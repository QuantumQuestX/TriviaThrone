package de.quantumquest.triviathrone.api.game;

import java.io.IOException;
import java.io.OutputStream;

public interface GameFileWriter {
    OutputStream createFile(String relativePath) throws IOException;
}
