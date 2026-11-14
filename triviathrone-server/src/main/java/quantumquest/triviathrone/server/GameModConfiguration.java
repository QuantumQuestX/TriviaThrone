package quantumquest.triviathrone.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import quantumquest.triviathrone.mod.GameModManager;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Creates game-mod infrastructure beans from the configured mod directory.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
public class GameModConfiguration {
    /**
     * Loads game mods from the configured directory.
     *
     * @return the configured game-mod manager
     */
    @Bean(destroyMethod = "close")
    public GameModManager gameModManager(
            @Value("${triviathrone.mods.games-directory:${triviathrone.plugin.directory:mods/games}}") String modsDir
    ) throws IOException {
        return GameModManager.load(Path.of(modsDir));
    }

    /**
     * Creates the registry backed by the loaded game mods.
     *
     * @param gameModManager the game mod manager
     * @return the created the registry backed by the loaded game mods
     */
    @Bean
    public GameProviderRegistry gameProviderRegistry(GameModManager gameModManager) {
        return new GameProviderRegistry(gameModManager);
    }
}
