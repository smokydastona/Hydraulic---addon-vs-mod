package org.geysermc.hydraulic.compat;

import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CompatibilityReport {
    private final String generatedAt;
    private final MetadataIndex.Summary metadata;
    private final Map<String, CompatibilityProfile> mods;

    public CompatibilityReport(
        @NotNull String generatedAt,
        @NotNull MetadataIndex.Summary metadata,
        @NotNull Map<String, CompatibilityProfile> mods
    ) {
        this.generatedAt = generatedAt;
        this.metadata = metadata;
        this.mods = Collections.unmodifiableMap(new LinkedHashMap<>(mods));
    }

    @NotNull
    public static CompatibilityReport empty() {
        return new CompatibilityReport("", MetadataIndex.Summary.empty(), Map.of());
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
    public Map<String, CompatibilityProfile> mods() {
        return this.mods;
    }
}