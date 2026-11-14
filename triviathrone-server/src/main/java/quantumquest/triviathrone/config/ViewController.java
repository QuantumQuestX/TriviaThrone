package quantumquest.triviathrone.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Routes browser-friendly paths to their static application views.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Controller
public class ViewController {
    /**
     * Returns the main-page view name.
     *
     * @return the main-page view name
     */
    @GetMapping("/")
    public String root() {
        return "forward:/index.html";
    }

    /**
     * Returns the administrator-page view name.
     *
     * @return the administrator-page view name
     */
    @GetMapping("/admin")
    public String admin() {
        return "forward:/admin.html";
    }

    /**
     * Authenticates credentials and writes a refresh-token cookie for the new session.
     *
     * @return the authenticated session
     */
    @GetMapping("/login")
    public String login() {
        return "forward:/login.html";
    }

    /**
     * Returns the lobby-page view name.
     *
     * @return the lobby-page view name
     */
    @GetMapping("/lobby")
    public String lobby() {
        return "forward:/lobby.html";
    }

    /**
     * Returns the game-creator-page view name.
     *
     * @return the game-creator-page view name
     */
    @GetMapping("/game-creator")
    public String gameCreator() {
        return "forward:/game-creator.html";
    }

    /**
     * Returns the account-page view name.
     *
     * @return the account-page view name
     */
    @GetMapping("/account")
    public String account() {
        return "redirect:/?page=account";
    }
}
