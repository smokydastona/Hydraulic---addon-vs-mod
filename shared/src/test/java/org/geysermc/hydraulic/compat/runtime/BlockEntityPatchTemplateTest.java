package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.geysermc.hydraulic.compat.MappingOwnership;
import org.geysermc.hydraulic.compat.mapping.ContentPatch;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockEntityPatchTemplateTest {
    @Test
    void resolvesTypedBedrockBlockEntityMutations() {
        BlockEntityPatchTemplate template = BlockEntityPatchTemplate.resolve(List.of(new ContentPatch(
            Identifier.fromNamespaceAndPath("example", "test_block_entity"),
            "block_entity",
            Map.of(
                "bedrock.block_entity.id", "Barrel",
                "bedrock.block_entity.data.CustomName", "Hydraulic Barrel",
                "bedrock.block_entity.data.isMovable", "true",
                "bedrock.block_entity.data.TransferCooldown", "8",
                "bedrock.block_entity.data.front_text.page", "1",
                "bedrock.block_entity.data.Items.0.Count", "3"
            ),
            MappingOwnership.USER,
            "user/block-entities.json",
            MappingOwnership.USER.priority(),
            0
        )));

        assertNotNull(template);
        assertEquals("Barrel", template.bedrockIdentifier());
        assertEquals(5, template.mutations().size());
        Map<List<String>, BlockEntityPatchTemplate.TagValue.Kind> kindsByPath = new LinkedHashMap<>();
        for (BlockEntityPatchTemplate.TagMutation mutation : template.mutations()) {
            kindsByPath.put(mutation.path(), mutation.value().kind());
        }
        assertEquals(BlockEntityPatchTemplate.TagValue.Kind.STRING, kindsByPath.get(List.of("CustomName")));
        assertEquals(BlockEntityPatchTemplate.TagValue.Kind.BOOLEAN, kindsByPath.get(List.of("isMovable")));
        assertEquals(BlockEntityPatchTemplate.TagValue.Kind.INTEGER, kindsByPath.get(List.of("TransferCooldown")));
        assertEquals(BlockEntityPatchTemplate.TagValue.Kind.INTEGER, kindsByPath.get(List.of("front_text", "page")));
        assertEquals(BlockEntityPatchTemplate.TagValue.Kind.INTEGER, kindsByPath.get(List.of("Items", "0", "Count")));
    }

    @Test
    void resolvesJavaSourceCopyMutations() {
        BlockEntityPatchTemplate template = BlockEntityPatchTemplate.resolve(List.of(new ContentPatch(
            Identifier.fromNamespaceAndPath("example", "test_block_entity"),
            "block_entity",
            Map.of(
                "bedrock.block_entity.data.CustomName", "$java.CustomName",
                "bedrock.block_entity.data.front_text.page", "$java.front_text.page"
            ),
            MappingOwnership.USER,
            "user/block-entities.json",
            MappingOwnership.USER.priority(),
            0
        )));

        assertNotNull(template);
        assertEquals(2, template.mutations().size());
        Map<List<String>, BlockEntityPatchTemplate.TagValue> valuesByPath = new LinkedHashMap<>();
        for (BlockEntityPatchTemplate.TagMutation mutation : template.mutations()) {
            valuesByPath.put(mutation.path(), mutation.value());
        }
        assertEquals(BlockEntityPatchTemplate.TagValue.Kind.COPY_FROM_JAVA, valuesByPath.get(List.of("CustomName")).kind());
        assertEquals(List.of("CustomName"), valuesByPath.get(List.of("CustomName")).javaSourcePath());
        assertEquals(List.of("front_text", "page"), valuesByPath.get(List.of("front_text", "page")).javaSourcePath());
    }

    @Test
    void laterPatchesOverrideEarlierValues() {
        BlockEntityPatchTemplate template = BlockEntityPatchTemplate.resolve(List.of(
            new ContentPatch(
                Identifier.fromNamespaceAndPath("example", "test_block_entity"),
                "block_entity",
                Map.of("bedrock.block_entity.data.TransferCooldown", "4"),
                MappingOwnership.BUILTIN,
                "builtin/base.json",
                MappingOwnership.BUILTIN.priority(),
                0
            ),
            new ContentPatch(
                Identifier.fromNamespaceAndPath("example", "test_block_entity"),
                "block_entity",
                Map.of("bedrock.block_entity.data.TransferCooldown", "8"),
                MappingOwnership.USER,
                "user/override.json",
                MappingOwnership.USER.priority(),
                0
            )
        ));

        assertNotNull(template);
        assertEquals(1, template.mutations().size());
        assertEquals(8, template.mutations().getFirst().value().value());
    }

    @Test
    void returnsNullWithoutExplicitBedrockBlockEntityPatchKeys() {
        BlockEntityPatchTemplate template = BlockEntityPatchTemplate.resolve(List.of(new ContentPatch(
            Identifier.fromNamespaceAndPath("example", "test_block_entity"),
            "block_entity",
            Map.of("visual.geometry", "example:test"),
            MappingOwnership.USER,
            "user/block-entities.json",
            MappingOwnership.USER.priority(),
            0
        )));

        assertNull(template);
        assertTrue(!BlockEntityPatchTemplate.supports(List.of()));
    }
}