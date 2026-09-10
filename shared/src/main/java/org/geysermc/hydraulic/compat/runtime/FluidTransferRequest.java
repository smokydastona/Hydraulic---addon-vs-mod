package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record FluidTransferRequest(
    @NotNull Identifier blockIdentifier,
    @NotNull TransferDirection direction,
    @NotNull TransferBridgeFactory.FluidStackView fluid,
    int tank,
    @Nullable String side
) {
    public FluidTransferRequest {
        if (tank < 0) {
            throw new IllegalArgumentException("Transfer tank must not be negative");
        }
        if (fluid.amount() <= 0) {
            throw new IllegalArgumentException("Fluid transfer requests require a positive amount");
        }
    }
}
