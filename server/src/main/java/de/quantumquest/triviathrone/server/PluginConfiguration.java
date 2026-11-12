package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.plugin.PluginManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;

@Configuration(proxyBeanMethods = false)
public class PluginConfiguration {
    @Bean(destroyMethod = "close")
    public PluginManager pluginManager(
            @Value("${triviathrone.plugin.directory:mods}") String modsDir
    ) throws IOException {
        return PluginManager.load(Path.of(modsDir));
    }

    @Bean
    public GameProviderRegistry gameProviderRegistry(PluginManager pluginManager) {
        return new GameProviderRegistry(pluginManager);
    }
}
