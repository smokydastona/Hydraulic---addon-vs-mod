package org.geysermc.hydraulic.compat.analysis;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.ContentInventory;
import org.geysermc.hydraulic.compat.MappingOwnership;
import org.geysermc.hydraulic.compat.mapping.ContentPatch;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockEntityAnalyzerTest {
    @Test
    void exposesMetadataBackedBlockEntityDataBridge() {
        ContentPatch patch = new ContentPatch(
            Identifier.fromNamespaceAndPath("example", "test_block_entity"),
            "block_entity",
            Map.of(
                "bedrock.block_entity.id", "Barrel",
                "bedrock.block_entity.data.CustomName", "Hydraulic Barrel",
                "bedrock.block_entity.data.isMovable", "true",
                "bedrock.block_entity.data.TransferCooldown", "8"
            ),
            MappingOwnership.USER,
            "user/block-entities.json",
            MappingOwnership.USER.priority(),
            0
        );
        MetadataIndex metadataIndex = new MetadataIndex(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(patch.target(), List.of(patch)), List.of(), MetadataIndex.Summary.empty());
        ContentInventory.ContentDescriptor descriptor = new ContentInventory.ContentDescriptor("block_entity", "example", "example:test_block_entity", true, false, List.of());

        CompatibilityObject object = new BlockEntityAnalyzer().analyze(descriptor, emptyInventory(), metadataIndex);

        assertEquals(SupportLevel.ADAPTED, object.supportResults().get("state_data").level());
        assertFalse(object.runtimeRequirements().contains("block_entity_data_bridge"));
        assertTrue(object.runtimeRequirements().contains("block_entity_interaction_bridge"));
        assertTrue(object.runtimeRequirements().contains("block_entity_behavior_bridge"));
    }

    @Test
    void keepsDataBridgeRequirementWhenNoPatchTemplateExists() {
        ContentInventory.ContentDescriptor descriptor = new ContentInventory.ContentDescriptor("block_entity", "example", "example:test_block_entity", true, false, List.of());

        CompatibilityObject object = new BlockEntityAnalyzer().analyze(descriptor, emptyInventory(), MetadataIndex.empty());

        assertEquals(SupportLevel.UNSUPPORTED, object.supportResults().get("state_data").level());
        assertTrue(object.runtimeRequirements().contains("block_entity_data_bridge"));
    }

    private static ContentInventory.ModContentInventory emptyInventory() {
        return new ContentInventory.ModContentInventory(
            "example",
            "example",
            "Example",
            "1.0.0",
            List.of(),
            new org.geysermc.hydraulic.compat.model.ModFingerprint(
                "example",
                "example",
                "1.0.0",
                "test",
                "1.21.0",
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
            ),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of()
        );
    }
}