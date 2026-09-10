package org.geysermc.hydraulic.fabric.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;

@Mixin(targets = "org.geysermc.geyser.platform.fabric.GeyserFabricPlatform")
public class GeyserFabricPlatformMixin {
    @Inject(method = "resolveResource", at = @At("RETURN"), cancellable = true)
    private void resolveResourceFallback(String path, CallbackInfoReturnable<InputStream> cir) {
        if (cir.getReturnValue() != null) {
            return;
        }

        InputStream resourceStream = resourceStream(GeyserFabricPlatformMixin.class.getClassLoader(), path);
        if (resourceStream == null) {
            resourceStream = resourceStream(Thread.currentThread().getContextClassLoader(), path);
        }

        if (resourceStream != null) {
            cir.setReturnValue(resourceStream);
        }
    }

    @Nullable
    private static InputStream resourceStream(@Nullable ClassLoader classLoader, String path) {
        return classLoader != null ? classLoader.getResourceAsStream(path) : null;
    }
}