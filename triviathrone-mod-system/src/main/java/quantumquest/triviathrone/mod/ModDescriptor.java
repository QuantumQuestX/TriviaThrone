package quantumquest.triviathrone.mod;

import java.util.List;

/**
 * Immutable metadata declared by a game mod's {@code mod.xml}.
 *
 * @param id              unique mod identifier
 * @param name            display name
 * @param version         mod version
 * @param apiVersion      game API version required by the mod
 * @param apiMinimum      optional inclusive minimum compatible API version
 * @param apiMaximumExclusive optional exclusive maximum compatible API version
 * @param description     human-readable mod description
 * @param providerClasses provider implementation class names declared by the mod
 * @author TiJ
 * @since 2.0.0
 */
public record ModDescriptor(
        String id,
        String name,
        String version,
        String apiVersion,
        String apiMinimum,
        String apiMaximumExclusive,
        String description,
        List<String> providerClasses
) {
    public ModDescriptor(
            String id,
            String name,
            String version,
            String apiVersion,
            String description,
            List<String> providerClasses
    ) {
        this(id, name, version, apiVersion, null, null, description, providerClasses);
    }

    /**
     * Creates metadata and snapshots the declared provider class names.
     *
     * @param id              unique mod identifier
     * @param name            display name
     * @param version         mod version
     * @param apiVersion      game API version required by the mod
     * @param description     human-readable mod description
     * @param providerClasses provider implementation class names
     */
    public ModDescriptor {
        providerClasses = List.copyOf(providerClasses);
    }
}
