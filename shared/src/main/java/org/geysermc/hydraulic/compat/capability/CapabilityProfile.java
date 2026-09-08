package org.geysermc.hydraulic.compat.capability;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record CapabilityProfile(
    @NotNull String objectId,
    @NotNull List<CapabilityRequirement> requirements,
    @NotNull List<CapabilityResult> results
) {
    public CapabilityProfile {
        requirements = List.copyOf(requirements);
        results = List.copyOf(results);
    }
}