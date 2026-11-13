package quantumquest.triviathrone.mod;

import quantumquest.triviathrone.api.game.IGameProvider;

import java.util.Objects;

public record RegisteredGameProvider(IGameProvider provider, ModDescriptor mod) {
    public RegisteredGameProvider {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(mod, "mod");
    }
}
