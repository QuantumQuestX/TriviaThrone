package quantumquest.triviathrone.server.game.creation;

import quantumquest.triviathrone.api.game.GameFileWriter;

import java.io.Closeable;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

final class DirectoryGameFileWriter implements GameFileWriter, Closeable {
    private final Path root;
    private final Set<OutputStream> openStreams = new HashSet<>();

    DirectoryGameFileWriter(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public OutputStream createFile(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank() || relativePath.contains("\\")) {
            throw new IllegalArgumentException("Invalid game-file path.");
        }

        Path relative = Path.of(relativePath);
        if (relative.isAbsolute() || relative.getNameCount() == 0) {
            throw new IllegalArgumentException("Game-file paths must be relative.");
        }

        for (Path component : relative) {
            if (component.toString().equals("..") || component.toString().equals(".")) {
                throw new IllegalArgumentException("Invalid game-file path.");
            }
        }

        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root) || target.equals(root)) {
            throw new IllegalArgumentException("Invalid game-file path.");
        }

        Files.createDirectories(target.getParent());
        OutputStream delegate = Files.newOutputStream(
                target,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );

        OutputStream tracked = new FilterOutputStream(delegate) {
            private boolean closed;

            @Override
            public void close() throws IOException {
                if (closed) {
                    return;
                }

                closed = true;

                try {
                    super.close();
                } finally {
                    openStreams.remove(this);
                }
            }
        };
        openStreams.add(tracked);

        return tracked;
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;

        for (OutputStream stream : new ArrayList<>(openStreams)) {
            try {
                stream.close();
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }

        if (failure != null) {
            throw failure;
        }
    }
}
