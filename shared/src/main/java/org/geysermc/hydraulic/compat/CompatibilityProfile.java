package org.geysermc.hydraulic.compat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record CompatibilityProfile(
    @NotNull String modId,
    @NotNull CompatibilityStatus overallStatus,
    @NotNull Map<String, CapabilityMetric> capabilities,
    @NotNull List<String> notes
) {
    public CompatibilityProfile {
        capabilities = Collections.unmodifiableMap(new LinkedHashMap<>(capabilities));
        notes = List.copyOf(notes);
    }

    public record CapabilityMetric(
        @Nullable Integer total,
        @Nullable Integer covered,
        @Nullable Integer coveragePercent,
        @NotNull CompatibilityStatus status,
        @NotNull String basis
    ) {
    }
}