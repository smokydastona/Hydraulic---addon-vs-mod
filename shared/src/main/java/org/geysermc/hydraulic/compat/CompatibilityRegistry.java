package org.geysermc.hydraulic.compat;

import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.geysermc.hydraulic.compat.runtime.RuntimeDispatchTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CompatibilityRegistry {
    private final MetadataIndex metadataIndex;
    private final MappingResolver mappingResolver;
    private final ContentInventory inventory;
    private final CompatibilityReport report;
    private final RuntimeDispatchTable dispatchTable;

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
        this.dispatchTable = RuntimeDispatchTable.compile(report, mappingResolver);
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
    public RuntimeDispatchTable dispatchTable() {
        return this.dispatchTable;
    }

    @Nullable
    public CompiledCompatibilityPlan plan(@NotNull String contentType, @NotNull String javaIdentifier) {
        return this.dispatchTable.plan(contentType, javaIdentifier);
    }

    @NotNull
    public CompatibilityRegistry withReport(@NotNull CompatibilityReport report) {
        return new CompatibilityRegistry(this.metadataIndex, this.mappingResolver, this.inventory, report);
    }
}