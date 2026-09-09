package org.geysermc.hydraulic.compat.runtime;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtType;
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

    @Test
    void copiesIndexedJavaListValuesIntoBedrockTag() {
        var translator = BlockEntityPatchTranslatorFactory.create(plan(Map.of(
            "bedrock.block_entity.data.FirstLine", "$java.messages.0",
            "bedrock.block_entity.data.SecondPageText", "$java.pages.1.text"
        )));

        assertNotNull(translator);

        NbtMapBuilder javaTagBuilder = NbtMap.builder();
        javaTagBuilder.put("messages", new NbtList<>(NbtType.STRING, List.of("alpha", "beta")));
        javaTagBuilder.put("pages", new NbtList<>(NbtType.COMPOUND, List.of(
            NbtMap.builder().putString("text", "page-0").build(),
            NbtMap.builder().putString("text", "page-1").build()
        )));
        NbtMap javaTag = javaTagBuilder.build();
        NbtMapBuilder bedrockTag = NbtMap.builder();

        translator.translateTag(null, bedrockTag, javaTag, null);

        NbtMap translated = bedrockTag.build();
        assertEquals("alpha", translated.getString("FirstLine"));
        assertEquals("page-1", translated.getString("SecondPageText"));
    }

    @Test
    void ignoresInvalidJavaListIndexes() {
        var translator = BlockEntityPatchTranslatorFactory.create(plan(Map.of(
            "bedrock.block_entity.data.FirstLine", "$java.messages.4",
            "bedrock.block_entity.data.SecondLine", "$java.messages.invalid"
        )));

        assertNotNull(translator);

        NbtMapBuilder bedrockTag = NbtMap.builder();
        bedrockTag.putString("FirstLine", "keep-me");
        bedrockTag.putString("SecondLine", "keep-me-too");

        translator.translateTag(
            null,
            bedrockTag,
            javaTag("messages", new NbtList<>(NbtType.STRING, List.of("alpha", "beta"))),
            null
        );

        NbtMap translated = bedrockTag.build();
        assertEquals("keep-me", translated.getString("FirstLine"));
        assertEquals("keep-me-too", translated.getString("SecondLine"));
    }

    @Test
    void writesIndexedBedrockListPaths() {
        Map<String, String> operations = new java.util.LinkedHashMap<>();
        operations.put("bedrock.block_entity.data.Items.0.Name", "$java.messages.0");
        operations.put("bedrock.block_entity.data.Items.0.Count", "3");
        operations.put("bedrock.block_entity.data.Items.1.Name", "$java.messages.1");
        operations.put("bedrock.block_entity.data.Items.1.Count", "7");
        var translator = BlockEntityPatchTranslatorFactory.create(plan(operations));

        assertNotNull(translator);

        NbtMapBuilder javaTagBuilder = NbtMap.builder();
        javaTagBuilder.put("messages", new NbtList<>(NbtType.STRING, List.of("alpha", "beta")));
        NbtMapBuilder bedrockTag = NbtMap.builder();

        translator.translateTag(null, bedrockTag, javaTagBuilder.build(), null);

        NbtMap translated = bedrockTag.build();
        Object itemsValue = translated.get("Items");
        assertTrue(itemsValue instanceof NbtList<?>);
        NbtList<?> items = (NbtList<?>) itemsValue;
        assertEquals(2, items.size());
        assertEquals("alpha", ((NbtMap) items.get(0)).getString("Name"));
        assertEquals(3, ((NbtMap) items.get(0)).getInt("Count"));
        assertEquals("beta", ((NbtMap) items.get(1)).getString("Name"));
        assertEquals(7, ((NbtMap) items.get(1)).getInt("Count"));
    }

    private static NbtMap javaTag(String key, Object value) {
        NbtMapBuilder builder = NbtMap.builder();
        builder.put(key, value);
        return builder.build();
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