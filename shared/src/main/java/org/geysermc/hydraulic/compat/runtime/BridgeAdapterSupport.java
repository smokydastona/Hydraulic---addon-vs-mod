package org.geysermc.hydraulic.compat.runtime;

import org.geysermc.hydraulic.compat.adapter.AdapterFeature;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.jetbrains.annotations.Nullable;

final class BridgeAdapterSupport {
    private BridgeAdapterSupport() {
    }

    static boolean supportsMenuFallback(@Nullable CompiledCompatibilityPlan plan) {
        return plan != null
            && plan.hasMenuFallback()
            && plan.requiresRuntimeBridge(RuntimeBridgeKind.MENU_CONTAINER)
            && plan.supportsAdapterFeature(AdapterFeature.MENU_FALLBACK_TRANSLATION);
    }

    static boolean supportsBlockEntityPatch(@Nullable CompiledCompatibilityPlan plan) {
        return plan != null
            && plan.hasBlockEntityPatch()
            && plan.requiresRuntimeBridge(RuntimeBridgeKind.BLOCK_ENTITY_DATA)
            && plan.supportsAdapterFeature(AdapterFeature.BLOCK_ENTITY_PATCH_TRANSLATION);
    }

    static boolean supportsEntityInteractionPrompt(@Nullable CompiledCompatibilityPlan plan) {
        return plan != null
            && plan.interactionPrompt() != null
            && !plan.interactionPrompt().isBlank()
            && plan.supportsAdapterFeature(AdapterFeature.ENTITY_INTERACTION_PROMPT);
    }

    static boolean supportsFluidTranslator(@Nullable CompiledCompatibilityPlan plan) {
        return plan != null
            && plan.supportsAdapterFeature(AdapterFeature.FLUID_BUCKET_TEXTURE_FALLBACK)
            && plan.inventoryFacts().containsKey("bucket_texture");
    }

    static boolean supportsFluidRuntime(@Nullable CompiledCompatibilityPlan plan) {
        return plan != null
            && plan.requiresFluidRuntime()
            && plan.behaviorTag() != null
            && !plan.behaviorTag().isBlank();
    }

    static boolean supportsItemTransfer(@Nullable CompiledCompatibilityPlan plan) {
        return plan != null
            && plan.runtimeBridgeKinds().contains(RuntimeBridgeKind.ITEM_TRANSFER);
    }

    static boolean supportsFluidTransfer(@Nullable CompiledCompatibilityPlan plan) {
        return plan != null
            && plan.runtimeBridgeKinds().contains(RuntimeBridgeKind.FLUID_TRANSFER);
    }

    static boolean supportsEnergyTransfer(@Nullable CompiledCompatibilityPlan plan) {
        return plan != null
            && plan.runtimeBridgeKinds().contains(RuntimeBridgeKind.ENERGY_TRANSFER);
    }

    static boolean supportsMachineBehavior(@Nullable CompiledCompatibilityPlan plan) {
        return plan != null
            && plan.runtimeBridgeKinds().contains(RuntimeBridgeKind.MACHINE_BEHAVIOR);
    }

    static boolean supportsMachineInventory(@Nullable CompiledCompatibilityPlan plan) {
        return plan != null
            && plan.runtimeBridgeKinds().contains(RuntimeBridgeKind.MACHINE_INVENTORY);
    }
}