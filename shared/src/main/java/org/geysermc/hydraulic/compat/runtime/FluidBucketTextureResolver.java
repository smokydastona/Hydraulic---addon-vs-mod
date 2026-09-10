package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BucketItem;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.adapter.AdapterFeature;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FluidBucketTextureResolver {
    private FluidBucketTextureResolver() {
    }

    @Nullable
    public static String resolve(@NotNull CompatibilityRegistry compatibilityRegistry, @NotNull BucketItem bucketItem) {
        Identifier fluidIdentifier = BuiltInRegistries.FLUID.getKey(bucketItem.getContent());
        if (fluidIdentifier == null) {
            return null;
        }

        return resolve(compatibilityRegistry.dispatchTable().fluid(fluidIdentifier));
    }

    @Nullable
    static String resolve(@Nullable CompiledCompatibilityPlan plan) {
        if (plan == null || !plan.supportsAdapterFeature(AdapterFeature.FLUID_BUCKET_TEXTURE_FALLBACK)) {
            return null;
        }

        String bucketTexture = plan.inventoryFacts().get("bucket_texture");
        return bucketTexture == null || bucketTexture.isBlank() ? null : bucketTexture;
    }
}