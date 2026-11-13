package de.quantumquest.triviathrone.mod;

import de.quantumquest.triviathrone.api.game.IGameProvider;

import java.util.Objects;

public record RegisteredGameProvider(IGameProvider provider, ModDescriptor mod) {
    public RegisteredGameProvider {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(mod, "mod");
    }
}
