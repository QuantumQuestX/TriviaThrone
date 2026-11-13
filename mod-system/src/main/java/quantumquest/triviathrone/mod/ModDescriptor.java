package quantumquest.triviathrone.mod;

import java.util.List;

public record ModDescriptor(
        String id,
        String name,
        String version,
        String apiVersion,
        String description,
        List<String> providerClasses
) {
    public ModDescriptor {
        providerClasses = List.copyOf(providerClasses);
    }
}
