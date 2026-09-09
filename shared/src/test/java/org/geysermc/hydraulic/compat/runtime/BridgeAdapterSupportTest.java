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
        assertTrue(BridgeAdapterSupport.supportsMenuFallback(menuPlan(true)));
        assertFalse(BridgeAdapterSupport.supportsMenuFallback(menuPlan(false)));
    }

    @Test
    void blockEntityPatchRequiresExplicitAdapterBinding() {
        assertTrue(BridgeAdapterSupport.supportsBlockEntityPatch(blockEntityPlan(true)));
        assertFalse(BridgeAdapterSupport.supportsBlockEntityPatch(blockEntityPlan(false)));
    }

    private static CompiledCompatibilityPlan menuPlan(boolean includeAdapter) {
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
            List.of("menu_behavior_bridge"),
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
            List.of("menu_behavior_bridge"),
            List.of(),
            null,
            false,
            SupportLevel.UNSUPPORTED,
            null
        );
    }

    private static CompiledCompatibilityPlan blockEntityPlan(boolean includeAdapter) {
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
            List.of("block_entity_behavior_bridge"),
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
            List.of("block_entity_behavior_bridge"),
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