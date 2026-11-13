package de.quantumquest.triviathrone.mod;

import de.quantumquest.triviathrone.api.game.IGameProvider;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Stream;

public final class GameModManager implements AutoCloseable {
    private final List<URLClassLoader> classLoaders = new ArrayList<>();
    private final List<IGameProvider> gameProviders = new ArrayList<>();
    private final List<RegisteredGameProvider> registeredGameProviders = new ArrayList<>();

    private GameModManager() {
    }

    public static GameModManager load(Path modsDirectory) throws IOException {
        Objects.requireNonNull(modsDirectory, "modsDirectory");
        Files.createDirectories(modsDirectory);

        GameModManager manager = new GameModManager();
        Set<String> gameTypes = new HashSet<>();
        Set<String> modIds = new HashSet<>();

        try (Stream<Path> entries = Files.list(modsDirectory)) {
            List<Path> jars = entries
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                    .sorted()
                    .toList();

            for (Path jar : jars) {
                manager.loadJar(jar, gameTypes, modIds);
            }

            return manager;
        } catch (IOException | RuntimeException | ServiceConfigurationError exception) {
            try {
                manager.close();
            } catch (IOException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    public List<IGameProvider> gameProviders() {
        return List.copyOf(gameProviders);
    }

    public List<RegisteredGameProvider> registeredGameProviders() {
        return List.copyOf(registeredGameProviders);
    }

    private void loadJar(Path jar, Set<String> gameTypes, Set<String> modIds) throws IOException {
        ModDescriptor descriptor = ModMetadataLoader.load(jar);
        if (modIds.contains(descriptor.id())) {
            throw new IllegalStateException("Duplicate game mod id: " + descriptor.id());
        }

        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{jar.toUri().toURL()},
                IGameProvider.class.getClassLoader()
        );

        try {
            List<IGameProvider> discovered = ServiceLoader
                    .load(IGameProvider.class, classLoader)
                    .stream()
                    .filter(provider -> provider.type().getClassLoader() == classLoader)
                    .map(ServiceLoader.Provider::get)
                    .toList();

            if (discovered.isEmpty()) {
                throw new IllegalStateException("No game providers found in game mod JAR.");
            }

            Set<String> declaredClasses = new HashSet<>(descriptor.providerClasses());
            Set<String> discoveredClasses = discovered.stream()
                    .map(provider -> provider.getClass().getName())
                    .collect(java.util.stream.Collectors.toSet());

            if (declaredClasses.size() != descriptor.providerClasses().size()
                    || !declaredClasses.equals(discoveredClasses)) {
                throw new IllegalStateException(
                        "Provider declarations in mod.xml do not match the JAR service descriptor."
                );
            }

            Set<String> jarGameTypes = new HashSet<>();
            for (IGameProvider provider : discovered) {
                String gameType = provider.gameType();
                if (gameType == null || gameType.isBlank()) {
                    throw new IllegalStateException("A provider has a blank game type.");
                }
                if (gameTypes.contains(gameType) || !jarGameTypes.add(gameType)) {
                    throw new IllegalStateException("Duplicate game type: " + gameType);
                }
            }

            modIds.add(descriptor.id());
            gameTypes.addAll(jarGameTypes);
            gameProviders.addAll(discovered);
            discovered.stream()
                    .map(provider -> new RegisteredGameProvider(provider, descriptor))
                    .forEach(registeredGameProviders::add);
            classLoaders.add(classLoader);
        } catch (RuntimeException | ServiceConfigurationError exception) {
            try {
                classLoader.close();
            } catch (IOException closeException) {
                exception.addSuppressed(closeException);
            }
            throw new IllegalStateException("Could not load game mod JAR: " + jar, exception);
        }
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;

        for (int i = classLoaders.size() - 1; i >= 0; i--) {
            try {
                classLoaders.get(i).close();
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }

        classLoaders.clear();
        gameProviders.clear();
        registeredGameProviders.clear();

        if (failure != null) {
            throw failure;
        }
    }
}
