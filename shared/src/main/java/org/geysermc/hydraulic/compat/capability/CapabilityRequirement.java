package org.geysermc.hydraulic.compat.capability;

import org.jetbrains.annotations.NotNull;

public record CapabilityRequirement(
    @NotNull Capability capability,
    boolean required
) {
}