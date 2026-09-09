package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.hydraulic.compat.CompatibilityStatus;
import org.geysermc.hydraulic.compat.MappingOwnership;
import org.geysermc.hydraulic.compat.adapter.AdapterBinding;
import org.geysermc.hydraulic.compat.adapter.AdapterFeature;
import org.geysermc.hydraulic.compat.ir.CompiledCompatibilityPlan;
import org.geysermc.hydraulic.compat.mapping.ContentPatch;
import org.geysermc.hydraulic.compat.model.Confidence;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BridgeAdapterSupportTest {
    @Test
    void menuFallbackRequiresExplicitAdapterBinding() {
        assertTrue(BridgeAdapterSupport.supportsMenuFallback(menuPlan(true, RuntimeBridgeKind.MENU_CONTAINER)));
        assertFalse(BridgeAdapterSupport.supportsMenuFallback(menuPlan(false, RuntimeBridgeKind.MENU_CONTAINER)));
    }

    @Test
    void menuFallbackRequiresContainerBridgeKind() {
        assertFalse(BridgeAdapterSupport.supportsMenuFallback(menuPlan(true, RuntimeBridgeKind.MENU_BEHAVIOR)));
    }

    @Test
    void blockEntityPatchRequiresExplicitAdapterBinding() {
        assertTrue(BridgeAdapterSupport.supportsBlockEntityPatch(blockEntityPlan(true, RuntimeBridgeKind.BLOCK_ENTITY_DATA)));
        assertFalse(BridgeAdapterSupport.supportsBlockEntityPatch(blockEntityPlan(false, RuntimeBridgeKind.BLOCK_ENTITY_DATA)));
    }

    @Test
    void blockEntityPatchRequiresDataBridgeKind() {
        assertFalse(BridgeAdapterSupport.supportsBlockEntityPatch(blockEntityPlan(true, RuntimeBridgeKind.BLOCK_ENTITY_BEHAVIOR)));
    }

    private static CompiledCompatibilityPlan menuPlan(boolean includeAdapter, RuntimeBridgeKind runtimeBridgeKind) {
        return new CompiledCompatibilityPlan(
            "testmod",
            "menu",
            Identifier.fromNamespaceAndPath("example", "test_menu").toString(),
            Identifier.fromNamespaceAndPath("example", "bedrock_menu").toString(),
            SupportLevel.ADAPTED,
            CompatibilityStatus.PARTIAL,
            75,
            new Confidence(0.8D, "test"),
            includeAdapter ? List.of(new AdapterBinding("menu.fallback_translator", AdapterFeature.MENU_FALLBACK_TRANSLATION, "test")) : List.of(),
            List.of(runtimeBridgeKind.requirementId()),
            List.of(runtimeBridgeKind),
            Map.of(),
            false,
            null,
            false,
            null,
            false,
            false,
            false,
            false,
            true,
            ContainerType.GENERIC_9X3,
            List.of(runtimeBridgeKind.requirementId()),
            List.of(),
            null,
            false,
            SupportLevel.UNSUPPORTED,
            null
        );
    }

    private static CompiledCompatibilityPlan blockEntityPlan(boolean includeAdapter, RuntimeBridgeKind runtimeBridgeKind) {
        return new CompiledCompatibilityPlan(
            "testmod",
            "block_entity",
            Identifier.fromNamespaceAndPath("example", "test_block_entity").toString(),
            Identifier.fromNamespaceAndPath("example", "test_block_entity").toString(),
            SupportLevel.ADAPTED,
            CompatibilityStatus.PARTIAL,
            75,
            new Confidence(0.8D, "test"),
            includeAdapter ? List.of(new AdapterBinding("block_entity.patch_translator", AdapterFeature.BLOCK_ENTITY_PATCH_TRANSLATION, "test")) : List.of(),
            List.of(runtimeBridgeKind.requirementId()),
            List.of(runtimeBridgeKind),
            Map.of(),
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
            List.of(),
            List.of(runtimeBridgeKind.requirementId()),
            BlockEntityPatchTemplate.resolve(List.of(new ContentPatch(
                Identifier.fromNamespaceAndPath("example", "test_block_entity"),
                "block_entity",
                Map.of("bedrock.block_entity.id", "BedrockChest"),
                MappingOwnership.USER,
                "test.json",
                1,
                0
            ))),
            true,
            SupportLevel.UNSUPPORTED,
            null
        );
    }
}