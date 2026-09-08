package org.geysermc.hydraulic.compat;

import org.geysermc.hydraulic.compat.model.CompatibilityFinding;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CompatibilityReport {
    private final String generatedAt;
    private final MetadataIndex.Summary metadata;
    private final List<CompatibilityFinding> metadataFindings;
    private final Map<String, CompatibilityProfile> mods;

    public CompatibilityReport(
        @NotNull String generatedAt,
        @NotNull MetadataIndex.Summary metadata,
        @NotNull List<CompatibilityFinding> metadataFindings,
        @NotNull Map<String, CompatibilityProfile> mods
    ) {
        this.generatedAt = generatedAt;
        this.metadata = metadata;
        this.metadataFindings = List.copyOf(metadataFindings);
        this.mods = Collections.unmodifiableMap(new LinkedHashMap<>(mods));
    }

    @NotNull
    public static CompatibilityReport empty() {
        return new CompatibilityReport("", MetadataIndex.Summary.empty(), List.of(), Map.of());
    }

    @NotNull
    public String generatedAt() {
        return this.generatedAt;
    }

    @NotNull
    public MetadataIndex.Summary metadata() {
        return this.metadata;
    }

    @NotNull
    public List<CompatibilityFinding> metadataFindings() {
        return this.metadataFindings;
    }

    @NotNull
    public Map<String, CompatibilityProfile> mods() {
        return this.mods;
    }
}