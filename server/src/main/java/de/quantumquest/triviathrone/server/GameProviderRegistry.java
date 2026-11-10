package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.api.game.IGameProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

@Component
public final class GameProviderRegistry {
    private final List<IGameProvider> providers;

    public GameProviderRegistry() {
        this(ServiceLoader.load(IGameProvider.class).stream()
                .map(ServiceLoader.Provider::get)
                .toList());
    }

    GameProviderRegistry(List<IGameProvider> providers) {
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
    }

    public Optional<IGameProvider> providerForFile(String filename) {
        if (filename == null) {
            return Optional.empty();
        }
        List<IGameProvider> matches = providers.stream()
                .filter(provider -> provider.supportsFile(filename))
                .toList();
        if (matches.size() > 1) {
            throw new IllegalStateException("Multiple game providers support file: " + filename);
        }
        return matches.stream().findFirst();
    }
}
