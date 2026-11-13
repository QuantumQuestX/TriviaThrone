package quantumquest.triviathrone.server;

import quantumquest.triviathrone.mod.GameModManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;

@Configuration(proxyBeanMethods = false)
public class GameModConfiguration {
    @Bean(destroyMethod = "close")
    public GameModManager gameModManager(
            @Value("${triviathrone.mods.games-directory:${triviathrone.plugin.directory:mods/games}}") String modsDir
    ) throws IOException {
        return GameModManager.load(Path.of(modsDir));
    }

    @Bean
    public GameProviderRegistry gameProviderRegistry(GameModManager gameModManager) {
        return new GameProviderRegistry(gameModManager);
    }
}
