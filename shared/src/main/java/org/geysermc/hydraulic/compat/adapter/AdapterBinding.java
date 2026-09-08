package org.geysermc.hydraulic.compat.adapter;

import org.jetbrains.annotations.NotNull;

public record AdapterBinding(
    @NotNull String adapterId,
    @NotNull AdapterFeature feature,
    @NotNull String reason
) {
}