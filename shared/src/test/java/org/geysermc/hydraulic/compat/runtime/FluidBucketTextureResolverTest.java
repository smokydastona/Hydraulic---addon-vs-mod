package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.CompatibilityStatus;
import org.geysermc.hydraulic.compat.adapter.AdapterBinding;
import org.geysermc.hydraulic.compat.adapter.AdapterFeature;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.geysermc.hydraulic.compat.model.Confidence;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class FluidBucketTextureResolverTest {
    @Test
    void resolvesTextureOnlyWhenAdapterBacked() {
        assertEquals("hydraulic_test_mod:barrel_pack", FluidBucketTextureResolver.resolve(plan(true, "hydraulic_test_mod:barrel_pack")));
        assertNull(FluidBucketTextureResolver.resolve(plan(false, "hydraulic_test_mod:barrel_pack")));
        assertNull(FluidBucketTextureResolver.resolve(plan(true, null)));
    }

    @Test
    void metadataEvidenceDoesNotAdvertiseExecutableFluidRuntime() {
        assertFalse(BridgeAdapterSupport.supportsFluidRuntime(plan(true, "hydraulic_test_mod:barrel_pack")));
    }

    private static CompiledCompatibilityPlan plan(boolean includeAdapter, String bucketTexture) {
        return new CompiledCompatibilityPlan(
            "testmod",
            "fluid",
            Identifier.fromNamespaceAndPath("example", "test_fluid").toString(),
            Identifier.fromNamespaceAndPath("example", "test_fluid").toString(),
            SupportLevel.ADAPTED,
            CompatibilityStatus.PARTIAL,
            75,
            new Confidence(0.8D, "test"),
            includeAdapter ? List.of(new AdapterBinding("fluid.bucket_texture_fallback", AdapterFeature.FLUID_BUCKET_TEXTURE_FALLBACK, "test")) : List.of(),
            List.of("fluid_runtime_bridge"),
            List.of(RuntimeBridgeKind.FLUID_RUNTIME),
            bucketTexture == null ? Map.of() : Map.of("bucket_texture", bucketTexture),
            false,
            null,
            false,
            null,
            false,
            false,
            false,
            false,
            false,
            null,
            null,
            List.of(),
            List.of(),
            List.of("fluid_runtime_bridge"),
            null,
            false,
            true,
            SupportLevel.UNSUPPORTED,
            null,
            List.of()
        );
    }
}