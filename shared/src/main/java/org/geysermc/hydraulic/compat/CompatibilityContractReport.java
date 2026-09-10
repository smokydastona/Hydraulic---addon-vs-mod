package org.geysermc.hydraulic.compat;

import org.geysermc.hydraulic.compat.model.CompatibilityContract;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persisted typed execution contracts for compatibility consumers and diagnostics.
 */
public record CompatibilityContractReport(
    @NotNull String generatedAt,
    @NotNull Map<String, List<CompatibilityContract>> mods
) {
    public CompatibilityContractReport {
        Map<String, List<CompatibilityContract>> copied = new LinkedHashMap<>();
        for (Map.Entry<String, List<CompatibilityContract>> entry : mods.entrySet()) {
            copied.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        mods = Map.copyOf(copied);
    }

    @NotNull
    public static CompatibilityContractReport from(@NotNull CompatibilityReport report) {
        Map<String, List<CompatibilityContract>> contracts = new LinkedHashMap<>();
        for (Map.Entry<String, CompatibilityProfile> entry : report.mods().entrySet()) {
            contracts.put(entry.getKey(), entry.getValue().objects().stream()
                .map(CompatibilityObject::contract)
                .toList());
        }
        return new CompatibilityContractReport(report.generatedAt(), contracts);
    }
}
