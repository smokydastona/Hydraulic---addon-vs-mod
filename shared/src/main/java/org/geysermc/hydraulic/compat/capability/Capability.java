package org.geysermc.hydraulic.compat.capability;

import org.jetbrains.annotations.NotNull;

public record Capability(
    @NotNull CapabilityDomain domain,
    @NotNull String name,
    @NotNull String description
) {
}