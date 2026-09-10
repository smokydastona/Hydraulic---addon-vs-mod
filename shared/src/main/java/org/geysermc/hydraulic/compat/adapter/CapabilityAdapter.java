package org.geysermc.hydraulic.compat.adapter;

import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface CapabilityAdapter {
    @NotNull String id();

    @NotNull Set<AdapterFeature> features();

    int priority();

    boolean supports(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature);

    @NotNull String reason(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature);
}
