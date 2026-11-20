package quantumquest.triviathrone.server;

import quantumquest.triviathrone.api.game.GameCreatorInfo;
import quantumquest.triviathrone.api.game.GameApi;
import quantumquest.triviathrone.api.game.GamePackageManifest;
import quantumquest.triviathrone.api.game.IGameCreator;
import quantumquest.triviathrone.api.game.IGameProvider;
import quantumquest.triviathrone.mod.GameModManager;
import quantumquest.triviathrone.mod.ModDescriptor;
import quantumquest.triviathrone.mod.RegisteredGameProvider;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Indexes installed game providers and resolves their capabilities.
 *
 * @author TiJ
 * @since 2.0.0
 */
public final class GameProviderRegistry {
    /**
     * Holds the registrations used by this component.
     */
    private final List<RegisteredGameProvider> registrations;

    /**
     * Creates a GameProviderRegistry.
     *
     * @param gameModManager the game mod manager
     */
    public GameProviderRegistry(GameModManager gameModManager) {
        this.registrations = List.copyOf(
            Objects.requireNonNull(gameModManager, "gameModManager").registeredGameProviders()
        );
    }

    /**
     * Creates a registry from providers, primarily for internal testing.
     *
     * @param providers the providers to register
     */
    GameProviderRegistry(List<IGameProvider> providers) {
        this.registrations = Objects.requireNonNull(providers, "providers").stream()
            .map(provider -> new RegisteredGameProvider(
                provider,
                    new ModDescriptor(
                    provider.gameType(),
                    provider.gameType(),
                    "test",
                    GameApi.CURRENT_VERSION,
                    "",
                    List.of(provider.getClass().getName())
                )
            ))
            .toList();
    }

    /**
     * Finds the unique provider that supports a game file name.
     *
     * @param filename the filename
     * @return the matching value
     */
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

    /**
     * Finds the provider registered for a game type.
     *
     * @param gameType the game type
     * @return the matching value
     */
    public Optional<IGameProvider> providerForType(String gameType) {
        if (gameType == null) {
            return Optional.empty();
        }

        return registrations.stream()
            .map(RegisteredGameProvider::provider)
            .filter(provider -> provider.gameType().equals(gameType))
            .findFirst();
    }

    /**
     * Finds the provider that matches a package manifest.
     *
     * @param manifest the manifest
     * @return the matching value
     */
    public Optional<IGameProvider> providerForPackage(GamePackageManifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        return registrations.stream()
            .filter(registration -> registration.mod().id().equals(manifest.modId()))
            .filter(registration -> GameApi.isCompatible(
                    manifest.apiVersion(), manifest.apiMaximumExclusive()))
            .map(RegisteredGameProvider::provider)
            .filter(provider -> provider.gameType().equals(manifest.gameType()))
            .filter(provider -> provider.getClass().getName().equals(manifest.providerClass()))
            .findFirst();
    }

    /**
     * Builds a package manifest for a registered game type.
     *
     * @param gameType      the game type
     * @param serverVersion the server version
     * @param metadata      the metadata stream
     * @param assets        the assets
     * @return the built a package manifest for a registered game type
     */
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
        ModDescriptor mod = registration.mod();
        return new GamePackageManifest(
            GamePackageManifest.CURRENT_MANIFEST_VERSION,
            mod.id(),
            mod.version(),
            mod.apiVersion(),
            provider.gameType(),
            provider.getClass().getName(),
            provider.gameDataFile(),
            provider.gameDataFormatVersion(),
            serverVersion,
            metadata,
            assets
        );
    }

    /**
     * Finds the optional creator for a game type.
     *
     * @param gameType the game type
     * @return the matching value
     */
    public Optional<IGameCreator> creatorForType(String gameType) {
        return providerForType(gameType).flatMap(IGameProvider::creator);
    }

    /**
     * Reports whether a game type supplies a custom editor.
     *
     * @param gameType the game type
     * @return whether s custom editor
     */
    public boolean hasCustomEditor(String gameType) {
        return providerForType(gameType)
            .flatMap(IGameProvider::uiDescriptor)
            .isPresent();
    }

    /**
     * Returns available game creators ordered by game type.
     *
     * @return available game creators ordered by game type
     */
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
