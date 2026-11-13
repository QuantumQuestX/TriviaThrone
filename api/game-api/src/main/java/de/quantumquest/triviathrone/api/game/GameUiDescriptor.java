package de.quantumquest.triviathrone.api.game;

import java.util.Objects;

/**
 * Declares the mod's editor and gameplay pages, relative to its {@code ui/} resource directory.
 */
public record GameUiDescriptor(String editorPage, String gamePage) {
    public GameUiDescriptor {
        Objects.requireNonNull(editorPage, "editorPage");
        Objects.requireNonNull(gamePage, "gamePage");
        validatePage(editorPage, "editorPage");
        validatePage(gamePage, "gamePage");
    }

    private static void validatePage(String page, String name) {
        if (page.isBlank()
                || page.startsWith("/")
                || page.contains("\\")
                || page.contains("..")
                || !page.endsWith(".html")) {
            throw new IllegalArgumentException(name + " must be a relative HTML resource path.");
        }
    }
}
