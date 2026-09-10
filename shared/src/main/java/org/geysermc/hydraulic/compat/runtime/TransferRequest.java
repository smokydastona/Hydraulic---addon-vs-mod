package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record TransferRequest(
    @NotNull Identifier blockIdentifier,
    @NotNull TransferDirection direction,
    @NotNull TransferBridgeFactory.ItemStackView item,
    int slot,
    @Nullable String side
) {
    public TransferRequest {
        if (slot < 0) {
            throw new IllegalArgumentException("Transfer slot must not be negative");
        }
        if (item.isEmpty()) {
            throw new IllegalArgumentException("Transfer requests require a non-empty item");
        }
    }
}
