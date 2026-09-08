package org.geysermc.hydraulic.metadata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record MetadataValidationIssue(
    @NotNull String code,
    @NotNull String severity,
    @NotNull String message,
    @Nullable String sourcePath,
    @Nullable String target
) {
}