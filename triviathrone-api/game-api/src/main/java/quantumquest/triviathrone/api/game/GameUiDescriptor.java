package quantumquest.triviathrone.api.game;

import java.util.Objects;

/**
 * Declares the mod's editor and gameplay pages, relative to its {@code ui/} resource directory.
 *
 * @param editorPage non-null relative HTML editor page
 * @param gamePage   non-null relative HTML gameplay page
 * @author TiJ
 * @since 2.0.0
 */
public record GameUiDescriptor(String editorPage, String gamePage) {
    /**
     * Validates the declared UI pages.
     *
     * @throws NullPointerException     if either page is null
     * @throws IllegalArgumentException if either page is not a relative HTML resource path
     */
    public GameUiDescriptor {
        Objects.requireNonNull(editorPage, "editorPage");
        Objects.requireNonNull(gamePage, "gamePage");
        validatePage(editorPage, "editorPage");
        validatePage(gamePage, "gamePage");
    }

    /**
     * Validates a page path.
     *
     * @param page non-null page path
     * @param name component name for error reporting
     * @throws IllegalArgumentException if the path is not a relative HTML resource path
     */
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
