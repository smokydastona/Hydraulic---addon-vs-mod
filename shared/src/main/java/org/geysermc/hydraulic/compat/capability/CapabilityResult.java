package org.geysermc.hydraulic.compat.capability;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record CapabilityResult(
    @NotNull Capability capability,
    boolean supported,
    @Nullable String details
) {
}