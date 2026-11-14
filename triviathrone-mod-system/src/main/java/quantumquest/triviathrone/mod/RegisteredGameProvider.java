package quantumquest.triviathrone.mod;

import quantumquest.triviathrone.api.game.IGameProvider;

import java.util.Objects;

/**
 * Associates a loaded game provider with the mod that supplied it.
 *
 * @param provider loaded game provider
 * @param mod      metadata of the supplying mod
 * @author TiJ
 * @since 2.0.0
 */
public record RegisteredGameProvider(IGameProvider provider, ModDescriptor mod) {
    /**
     * Creates an association with non-null provider and metadata.
     *
     * @param provider loaded game provider
     * @param mod      metadata of the supplying mod
     * @throws NullPointerException if either argument is {@code null}
     */
    public RegisteredGameProvider {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(mod, "mod");
    }
}
