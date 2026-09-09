package org.geysermc.hydraulic.compat.runtime;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import net.minecraft.resources.Identifier;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockEntityPatchTranslatorFactoryTest {
    @Test
    void copiesJavaTagValuesIntoBedrockTag() {
        var translator = BlockEntityPatchTranslatorFactory.create(plan(Map.of(
            "bedrock.block_entity.id", "BedrockChest",
            "bedrock.block_entity.data.CustomName", "$java.CustomName",
            "bedrock.block_entity.data.TransferCooldown", "$java.cooldowns.transfer",
            "bedrock.block_entity.data.front_text", "$java.front_text",
            "bedrock.block_entity.data.isMovable", "true"
        )));

        assertNotNull(translator);

        NbtMap javaTag = NbtMap.builder()
            .putString("CustomName", "Hydraulic Barrel")
            .putCompound("cooldowns", NbtMap.builder().putInt("transfer", 8).build())
            .putCompound("front_text", NbtMap.builder().putInt("page", 1).putString("text", "Hello").build())
            .build();
        NbtMapBuilder bedrockTag = NbtMap.builder();

        translator.translateTag(null, bedrockTag, javaTag, null);

        NbtMap translated = bedrockTag.build();
        assertEquals("BedrockChest", translated.getString("id"));
        assertEquals("Hydraulic Barrel", translated.getString("CustomName"));
        assertEquals(8, translated.getInt("TransferCooldown"));
        assertTrue(translated.getBoolean("isMovable"));
        NbtMap frontText = translated.getCompound("front_text");
        assertEquals(1, frontText.getInt("page"));
        assertEquals("Hello", frontText.getString("text"));
    }

    @Test
    void ignoresMissingJavaSourceValues() {
        var translator = BlockEntityPatchTranslatorFactory.create(plan(Map.of(
            "bedrock.block_entity.data.CustomName", "$java.CustomName"
        )));

        assertNotNull(translator);

        NbtMapBuilder bedrockTag = NbtMap.builder();
        bedrockTag.putString("CustomName", "Existing Name");

        translator.translateTag(null, bedrockTag, NbtMap.builder().build(), null);

        assertEquals("Existing Name", bedrockTag.build().getString("CustomName"));
    }

    @Test
    void requiresBlockEntityDataBridgeKind() {
        assertNull(BlockEntityPatchTranslatorFactory.create(plan(Map.of(
            "bedrock.block_entity.data.CustomName", "$java.CustomName"
        ), RuntimeBridgeKind.BLOCK_ENTITY_BEHAVIOR)));
    }

    private static CompiledCompatibilityPlan plan(Map<String, String> operations) {
        return plan(operations, RuntimeBridgeKind.BLOCK_ENTITY_DATA);
    }

    private static CompiledCompatibilityPlan plan(Map<String, String> operations, RuntimeBridgeKind runtimeBridgeKind) {
        return new CompiledCompatibilityPlan(
            "testmod",
            "block_entity",
            Identifier.fromNamespaceAndPath("example", "test_block_entity").toString(),
            Identifier.fromNamespaceAndPath("example", "test_block_entity").toString(),
            SupportLevel.ADAPTED,
            CompatibilityStatus.PARTIAL,
            75,
            new Confidence(0.8D, "test"),
            List.of(new AdapterBinding("block_entity.patch_translator", AdapterFeature.BLOCK_ENTITY_PATCH_TRANSLATION, "test")),
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
            List.of(),
            BlockEntityPatchTemplate.resolve(List.of(new ContentPatch(
                Identifier.fromNamespaceAndPath("example", "test_block_entity"),
                "block_entity",
                operations,
                MappingOwnership.USER,
                "test.json",
                1,
                0
            ))),
            true,
            false,
            SupportLevel.UNSUPPORTED,
            null
        );
    }
}