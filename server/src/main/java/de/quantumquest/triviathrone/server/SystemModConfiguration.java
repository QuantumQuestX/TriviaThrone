package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.auth.IUserAccountRepository;
import de.quantumquest.triviathrone.api.system.SystemProviderContext;
import de.quantumquest.triviathrone.mod.SystemModManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class SystemModConfiguration {
    @Bean(destroyMethod = "close")
    public SystemModManager systemModManager(
            @Value("${triviathrone.mods.system-directory:mods/system}") String modsDirectory,
            @Value("${triviathrone.system.data-directory:data/system}") String dataDirectory,
            ConfigurableEnvironment environment
    ) throws IOException {
        return SystemModManager.load(
                Path.of(modsDirectory),
                new SystemProviderContext(Path.of(dataDirectory), systemConfiguration(environment))
        );
    }

    @Bean
    public IUserAccountRepository userAccountRepository(SystemModManager systemMods) {
        return systemMods.find(IUserAccountRepository.class)
                .orElseThrow(() -> new IllegalStateException(
                        "No system mod registered an IUserAccountRepository."
                ));
    }

    private static Map<String, String> systemConfiguration(ConfigurableEnvironment environment) {
        Map<String, String> configuration = new LinkedHashMap<>();
        for (PropertySource<?> source : environment.getPropertySources()) {
            if (source instanceof EnumerablePropertySource<?> enumerable) {
                for (String name : enumerable.getPropertyNames()) {
                    if (name.startsWith("triviathrone.system.")) {
                        Object value = environment.getProperty(name);
                        if (value != null) {
                            configuration.putIfAbsent(name, String.valueOf(value));
                        }
                    }
                }
            }
        }
        return Map.copyOf(configuration);
    }
}
