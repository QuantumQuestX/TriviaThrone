package de.quantumquest.triviathrone.plugin;

import de.quantumquest.triviathrone.api.game.IGameProvider;

import java.util.Objects;

public record RegisteredGameProvider(IGameProvider provider, PluginDescriptor plugin) {
    public RegisteredGameProvider {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(plugin, "plugin");
    }
}
