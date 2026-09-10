package org.geysermc.hydraulic.compat.adapter;

import org.jetbrains.annotations.NotNull;

public record AdapterBinding(
    @NotNull String adapterId,
    @NotNull AdapterFeature feature,
    @NotNull String reason
) {
    @NotNull
    public static AdapterBinding unsupported(@NotNull String adapterId) {
        return new AdapterBinding(
            adapterId,
            AdapterFeature.UNSUPPORTED,
            "No adapter available for this capability"
        );
    }
}
