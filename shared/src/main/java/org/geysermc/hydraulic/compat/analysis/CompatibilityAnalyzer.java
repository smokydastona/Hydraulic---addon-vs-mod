package org.geysermc.hydraulic.compat.analysis;

import org.geysermc.hydraulic.compat.ContentInventory;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.jetbrains.annotations.NotNull;

public interface CompatibilityAnalyzer {
    @NotNull String kind();

    default boolean supports(@NotNull ContentInventory.ContentDescriptor descriptor) {
        return this.kind().equals(descriptor.kind());
    }

    @NotNull CompatibilityObject analyze(
        @NotNull ContentInventory.ContentDescriptor descriptor,
        @NotNull ContentInventory.ModContentInventory inventory,
        @NotNull MetadataIndex metadataIndex
    );
}