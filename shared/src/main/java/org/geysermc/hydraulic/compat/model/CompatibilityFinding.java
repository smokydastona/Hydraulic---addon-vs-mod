package org.geysermc.hydraulic.compat.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record CompatibilityFinding(
    @NotNull String code,
    @NotNull Severity severity,
    @NotNull String domain,
    @NotNull String message,
    @Nullable String reason,
    @Nullable String suggestedResolution,
    @Nullable String sourcePath
) {
    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }
}