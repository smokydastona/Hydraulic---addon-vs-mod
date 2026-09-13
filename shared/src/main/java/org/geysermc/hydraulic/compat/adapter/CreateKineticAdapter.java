package org.geysermc.hydraulic.compat.adapter;

import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Exception adapter for Create mod kinetics (speed, stress, kinetic state translation).
 */
public final class CreateKineticAdapter implements CapabilityAdapter {
    @Override
    public @NotNull String id() {
        return "adapter.create.kinetic";
    }

    @Override
    public @NotNull Set<AdapterFeature> features() {
        return Set.of(AdapterFeature.BLOCK_PLACEMENT);
    }

    @Override
    public boolean supports(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
        return compatibilityObject.javaIdentifier().startsWith("create:");
    }

    @Override
    public @NotNull String reason(@NotNull CompatibilityObject compatibilityObject, @Nullable Object runtimeObject, @NotNull AdapterFeature feature) {
        return "Create kinetic block state translation and rotational speed/stress bridging.";
    }

    @Override
    public int priority() {
        return 100;
    }
}
