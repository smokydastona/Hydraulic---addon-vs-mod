package org.geysermc.hydraulic.compat.adapter;

import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Exception adapter for Farmer's Delight cooking pot, cutting board, and container UI bridging.
 */
public final class FarmersDelightAdapter implements CapabilityAdapter {
    @Override
    public @NotNull String id() {
        return "adapter.farmersdelight.cooking_pot";
    }

    @Override
    public @NotNull Set<AdapterFeature> features() {
        return Set.of(AdapterFeature.MENU_FALLBACK_TRANSLATION);
    }

    @Override
    public boolean supports(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
        return compatibilityObject.javaIdentifier().startsWith("farmersdelight:");
    }

    @Override
    public @NotNull String reason(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
        return "Farmer's Delight cooking pot and cutting board container interaction bridging.";
    }

    @Override
    public int priority() {
        return 100;
    }
}
