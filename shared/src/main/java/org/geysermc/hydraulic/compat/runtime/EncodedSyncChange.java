package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record EncodedSyncChange(
    @NotNull Identifier blockIdentifier,
    @NotNull String field,
    @NotNull EncodedSyncKind kind,
    int slot,
    @Nullable String beforeItemId,
    int beforeCount,
    @Nullable String afterItemId,
    int afterCount,
    @Nullable Object beforeValue,
    @Nullable Object afterValue,
    @NotNull SyncPriority priority,
    @NotNull String sourceField
) {
}
