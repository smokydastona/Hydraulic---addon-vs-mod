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
}