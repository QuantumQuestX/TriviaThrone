package de.quantumquest.triviathrone.plugin;

import java.util.List;

public record PluginDescriptor(
        String id,
        String name,
        String version,
        String apiVersion,
        String description,
        List<String> providerClasses
) {
    public PluginDescriptor {
        providerClasses = List.copyOf(providerClasses);
    }
}
