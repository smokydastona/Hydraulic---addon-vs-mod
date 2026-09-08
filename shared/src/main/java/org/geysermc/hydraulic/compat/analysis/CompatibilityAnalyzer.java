package org.geysermc.hydraulic.compat.analysis;

import org.geysermc.hydraulic.compat.ContentInventory;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.jetbrains.annotations.NotNull;

public interface CompatibilityAnalyzer {
    boolean supports(@NotNull ContentInventory.ContentDescriptor descriptor);

    @NotNull CompatibilityObject analyze(
        @NotNull ContentInventory.ContentDescriptor descriptor,
        @NotNull ContentInventory.ModContentInventory inventory,
        @NotNull MetadataIndex metadataIndex
    );
}