package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.api.game.GameCreatorInfo;
import de.quantumquest.triviathrone.api.game.GamePackageManifest;
import de.quantumquest.triviathrone.api.game.IGameCreator;
import de.quantumquest.triviathrone.api.game.IGameProvider;
import de.quantumquest.triviathrone.plugin.PluginDescriptor;
import de.quantumquest.triviathrone.plugin.PluginManager;
import de.quantumquest.triviathrone.plugin.RegisteredGameProvider;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class GameProviderRegistry {
    private final List<RegisteredGameProvider> registrations;

    public GameProviderRegistry(PluginManager pluginManager) {
        this.registrations = List.copyOf(
                Objects.requireNonNull(pluginManager, "pluginManager").registeredGameProviders()
        );
    }

    GameProviderRegistry(List<IGameProvider> providers) {
        this.registrations = Objects.requireNonNull(providers, "providers").stream()
                .map(provider -> new RegisteredGameProvider(
                        provider,
                        new PluginDescriptor(
                                provider.gameType(),
                                provider.gameType(),
                                "test",
                                "test",
                                "",
                                List.of(provider.getClass().getName())
                        )
                ))
                .toList();
    }

    public Optional<IGameProvider> providerForFile(String filename) {
        if (filename == null) {
            return Optional.empty();
        }
        List<IGameProvider> matches = registrations.stream()
                .map(RegisteredGameProvider::provider)
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

        return registrations.stream()
                .map(RegisteredGameProvider::provider)
                .filter(provider -> provider.gameType().equals(gameType))
                .findFirst();
    }

    public Optional<IGameProvider> providerForPackage(GamePackageManifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        return registrations.stream()
                .filter(registration -> registration.plugin().id().equals(manifest.pluginId()))
                .filter(registration -> registration.plugin().apiVersion().equals(manifest.apiVersion()))
                .map(RegisteredGameProvider::provider)
                .filter(provider -> provider.gameType().equals(manifest.gameType()))
                .filter(provider -> provider.getClass().getName().equals(manifest.providerClass()))
                .findFirst();
    }

    public GamePackageManifest packageManifestForType(
            String gameType,
            String serverVersion,
            GamePackageManifest.PackageMetadata metadata,
            List<GamePackageManifest.Asset> assets
    ) {
        RegisteredGameProvider registration = registrations.stream()
                .filter(candidate -> candidate.provider().gameType().equals(gameType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown game type: " + gameType));
        IGameProvider provider = registration.provider();
        PluginDescriptor plugin = registration.plugin();
        return new GamePackageManifest(
                GamePackageManifest.CURRENT_MANIFEST_VERSION,
                plugin.id(),
                plugin.version(),
                plugin.apiVersion(),
                provider.gameType(),
                provider.getClass().getName(),
                provider.gameDataFile(),
                provider.gameDataFormatVersion(),
                serverVersion,
                metadata,
                assets
        );
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
        return registrations.stream()
                .map(RegisteredGameProvider::provider)
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
