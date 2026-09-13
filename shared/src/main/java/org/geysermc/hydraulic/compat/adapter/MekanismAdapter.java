package org.geysermc.hydraulic.compat.adapter;

import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Exception adapter for Mekanism multi-element industrial machines, chemical tanks, and energy bridging.
 */
public final class MekanismAdapter implements CapabilityAdapter {
    @Override
    public @NotNull String id() {
        return "adapter.mekanism.machine";
    }

    @Override
    public @NotNull Set<AdapterFeature> features() {
        return Set.of(AdapterFeature.BLOCK_PLACEMENT, AdapterFeature.BLOCK_ENTITY_PATCH_TRANSLATION);
    }

    @Override
    public boolean supports(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
        return compatibilityObject.javaIdentifier().startsWith("mekanism:");
    }

    @Override
    public @NotNull String reason(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
        return "Mekanism machine, gas/slurry tank, and energy network bridging.";
    }

    @Override
    public int priority() {
        return 100;
    }
}
