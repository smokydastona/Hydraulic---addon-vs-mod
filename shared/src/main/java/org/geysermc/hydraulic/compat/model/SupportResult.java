package org.geysermc.hydraulic.compat.model;

import org.geysermc.hydraulic.compat.CompatibilityStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record SupportResult(
    @NotNull String scope,
    @NotNull SupportLevel level,
    @NotNull CompatibilityStatus status,
    @Nullable Integer scorePercent,
    @NotNull List<String> supportedCapabilities,
    @NotNull List<String> missingCapabilities,
    @NotNull List<String> notes
) {
    public SupportResult {
        supportedCapabilities = List.copyOf(supportedCapabilities);
        missingCapabilities = List.copyOf(missingCapabilities);
        notes = List.copyOf(notes);
    }
}