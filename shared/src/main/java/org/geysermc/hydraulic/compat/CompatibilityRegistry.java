package org.geysermc.hydraulic.compat;

import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.jetbrains.annotations.NotNull;

public final class CompatibilityRegistry {
    private final MetadataIndex metadataIndex;
    private final MappingResolver mappingResolver;
    private final ContentInventory inventory;
    private final CompatibilityReport report;

    public CompatibilityRegistry(
        @NotNull MetadataIndex metadataIndex,
        @NotNull MappingResolver mappingResolver,
        @NotNull ContentInventory inventory,
        @NotNull CompatibilityReport report
    ) {
        this.metadataIndex = metadataIndex;
        this.mappingResolver = mappingResolver;
        this.inventory = inventory;
        this.report = report;
    }

    @NotNull
    public static CompatibilityRegistry empty() {
        MetadataIndex metadataIndex = MetadataIndex.empty();
        return new CompatibilityRegistry(metadataIndex, new MappingResolver(metadataIndex), ContentInventory.empty(), CompatibilityReport.empty());
    }

    @NotNull
    public MetadataIndex metadataIndex() {
        return this.metadataIndex;
    }

    @NotNull
    public MappingResolver mappingResolver() {
        return this.mappingResolver;
    }

    @NotNull
    public ContentInventory inventory() {
        return this.inventory;
    }

    @NotNull
    public CompatibilityReport report() {
        return this.report;
    }

    @NotNull
    public CompatibilityRegistry withReport(@NotNull CompatibilityReport report) {
        return new CompatibilityRegistry(this.metadataIndex, this.mappingResolver, this.inventory, report);
    }
}