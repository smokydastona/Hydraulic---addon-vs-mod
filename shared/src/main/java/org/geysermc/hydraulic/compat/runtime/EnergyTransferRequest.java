package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record EnergyTransferRequest(
    @NotNull Identifier blockIdentifier,
    @NotNull TransferDirection direction,
    int amount,
    @Nullable String side
) {
    public EnergyTransferRequest {
        if (amount <= 0) {
            throw new IllegalArgumentException("Energy transfer requests require a positive amount");
        }
    }
}
