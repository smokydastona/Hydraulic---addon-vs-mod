package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating fluid translation and runtime bridges based on compiled compatibility plans.
 * This factory consumes typed RuntimeBridgeKind categories instead of freeform requirement strings.
 */
public final class FluidBridgeFactory {
    private FluidBridgeFactory() {
    }

    /**
     * Checks if a fluid translator bridge can be created for the given fluid identifier.
     */
    public static boolean supportsTranslator(@NotNull Identifier fluidIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().fluid(fluidIdentifier);
        return BridgeAdapterSupport.supportsFluidTranslator(plan);
    }

    /**
     * Checks if a fluid runtime bridge can be created for the given fluid identifier.
     */
    public static boolean supportsRuntimeBridge(@NotNull Identifier fluidIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().fluid(fluidIdentifier);
        return BridgeAdapterSupport.supportsFluidRuntime(plan);
    }

    /**
     * Creates a fluid translator bridge if supported by the compiled plan.
     */
    @Nullable
    public static FluidTranslatorBridge createTranslator(@NotNull Identifier fluidIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().fluid(fluidIdentifier);
        if (!BridgeAdapterSupport.supportsFluidTranslator(plan)) {
            return null;
        }
        return new MetadataBackedFluidTranslatorBridge(plan);
    }

    /**
     * Creates a fluid runtime bridge if supported by the compiled plan.
     */
    @Nullable
    public static FluidRuntimeBridge createRuntimeBridge(@NotNull Identifier fluidIdentifier, @NotNull CompatibilityRegistry compatibilityRegistry) {
        CompiledCompatibilityPlan plan = compatibilityRegistry.dispatchTable().fluid(fluidIdentifier);
        if (!BridgeAdapterSupport.supportsFluidRuntime(plan)) {
            return null;
        }
        return new MetadataBackedFluidRuntimeBridge(plan);
    }

    /**
     * Interface for fluid translation bridges.
     */
    public interface FluidTranslatorBridge {
        @NotNull String bedrockIdentifier(@NotNull Identifier javaIdentifier);
        @Nullable String bucketTexture(@NotNull Identifier javaIdentifier);
    }

    /**
     * Interface for fluid runtime bridges.
     */
    public interface FluidRuntimeBridge {
        boolean hasRuntimeBehavior(@NotNull Identifier javaIdentifier);
        @Nullable String behaviorTag(@NotNull Identifier javaIdentifier);
    }

    private static final class MetadataBackedFluidTranslatorBridge implements FluidTranslatorBridge {
        private final CompiledCompatibilityPlan plan;

        private MetadataBackedFluidTranslatorBridge(@NotNull CompiledCompatibilityPlan plan) {
            this.plan = plan;
        }

        @Override
        @NotNull
        public String bedrockIdentifier(@NotNull Identifier javaIdentifier) {
            return this.plan.resolvedIdentifier();
        }

        @Override
        @Nullable
        public String bucketTexture(@NotNull Identifier javaIdentifier) {
            return this.plan.inventoryFacts().get("bucket_texture");
        }
    }

    private static final class MetadataBackedFluidRuntimeBridge implements FluidRuntimeBridge {
        private final CompiledCompatibilityPlan plan;

        private MetadataBackedFluidRuntimeBridge(@NotNull CompiledCompatibilityPlan plan) {
            this.plan = plan;
        }

        @Override
        public boolean hasRuntimeBehavior(@NotNull Identifier javaIdentifier) {
            return this.plan.requiresFluidRuntime();
        }

        @Override
        @Nullable
        public String behaviorTag(@NotNull Identifier javaIdentifier) {
            return this.plan.behaviorTag();
        }
    }
}