package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.api.game.GameCreatorInfo;
import de.quantumquest.triviathrone.api.game.IGameCreator;
import de.quantumquest.triviathrone.api.game.IGameProvider;
import de.quantumquest.triviathrone.plugin.PluginManager;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class GameProviderRegistry {
    private final List<IGameProvider> providers;

    public GameProviderRegistry(PluginManager pluginManager) {
        this(Objects.requireNonNull(pluginManager, "pluginManager").gameProviders());
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

    public Optional<IGameProvider> providerForType(String gameType) {
        if (gameType == null) {
            return Optional.empty();
        }

        return providers.stream()
                .filter(provider -> provider.gameType().equals(gameType))
                .findFirst();
    }

    public Optional<IGameCreator> creatorForType(String gameType) {
        return providerForType(gameType).flatMap(IGameProvider::creator);
    }

    public boolean hasCustomEditor(String gameType) {
        return providerForType(gameType)
                .flatMap(IGameProvider::uiDescriptor)
                .isPresent();
    }

    public List<GameCreatorInfo> creators() {
        return providers.stream()
                .flatMap(provider -> provider.creator()
                        .stream()
                        .map(creator -> new GameCreatorInfo(
                                provider.gameType(),
                                creator.descriptor()
                        )))
                .sorted(Comparator.comparing(GameCreatorInfo::gameType))
                .toList();
    }
}
