package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record SyncChange(
    @NotNull Identifier blockIdentifier,
    @NotNull String field,
    @Nullable Object before,
    @Nullable Object after,
    @NotNull SyncPriority priority,
    @Nullable RuntimeTraceId traceId
) {
    public SyncChange(
        @NotNull Identifier blockIdentifier,
        @NotNull String field,
        @Nullable Object before,
        @Nullable Object after,
        @NotNull SyncPriority priority
    ) {
        this(blockIdentifier, field, before, after, priority, null);
    }
}
