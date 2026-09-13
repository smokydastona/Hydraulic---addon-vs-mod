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
        return false;
    }

    static boolean supportsItemTransfer(@Nullable CompiledCompatibilityPlan plan) {
        return plan != null
            && plan.runtimeBridgeKinds().contains(RuntimeBridgeKind.ITEM_TRANSFER)
            && hasTrueFact(plan, "can_insert", "can_extract");
    }

    static boolean supportsFluidTransfer(@Nullable CompiledCompatibilityPlan plan) {
        return plan != null
            && plan.runtimeBridgeKinds().contains(RuntimeBridgeKind.FLUID_TRANSFER)
            && hasTrueFact(plan, "can_insert_fluid", "can_extract_fluid");
    }

    static boolean supportsEnergyTransfer(@Nullable CompiledCompatibilityPlan plan) {
        return plan != null
            && plan.runtimeBridgeKinds().contains(RuntimeBridgeKind.ENERGY_TRANSFER)
            && hasTrueFact(plan, "can_receive_energy", "can_provide_energy");
    }

    static boolean supportsMachineBehavior(@Nullable CompiledCompatibilityPlan plan) {
        return plan != null
            && plan.runtimeBridgeKinds().contains(RuntimeBridgeKind.MACHINE_BEHAVIOR)
            && hasTrueFact(plan, "has_processing");
    }

    static boolean supportsMachineInventory(@Nullable CompiledCompatibilityPlan plan) {
        return plan != null
            && plan.runtimeBridgeKinds().contains(RuntimeBridgeKind.MACHINE_INVENTORY)
            && hasTrueFact(plan, "has_inventory");
    }

    static boolean supportsAutomation(@Nullable CompiledCompatibilityPlan plan) {
        return plan != null
            && plan.runtimeBridgeKinds().contains(RuntimeBridgeKind.AUTOMATION_ACCESS)
            && (hasTrueFact(plan, "sided_insert", "sided_extract") || hasFact(plan, "filtering"));
    }

    private static boolean hasTrueFact(@Nullable CompiledCompatibilityPlan plan, String... keys) {
        if (plan == null) {
            return false;
        }
        for (String key : keys) {
            if (Boolean.parseBoolean(plan.inventoryFacts().getOrDefault(key, "false"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasFact(@Nullable CompiledCompatibilityPlan plan, String key) {
        if (plan == null) {
            return false;
        }
        String value = plan.inventoryFacts().get(key);
        return value != null && !value.isBlank();
    }
}