package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.hydraulic.compat.CompatibilityProfile;
import org.geysermc.hydraulic.compat.CompatibilityRegistry;
import org.geysermc.hydraulic.compat.CompatibilityReport;
import org.geysermc.hydraulic.compat.CompatibilityStatus;
import org.geysermc.hydraulic.compat.ContentInventory;
import org.geysermc.hydraulic.compat.MappingOwnership;
import org.geysermc.hydraulic.compat.MappingResolver;
import org.geysermc.hydraulic.compat.adapter.AdapterBinding;
import org.geysermc.hydraulic.compat.adapter.AdapterFeature;
import org.geysermc.hydraulic.compat.capability.CapabilityProfile;
import org.geysermc.hydraulic.compat.mapping.ContentPatch;
import org.geysermc.hydraulic.compat.model.CompatibilityObject;
import org.geysermc.hydraulic.compat.model.Confidence;
import org.geysermc.hydraulic.compat.model.ModFingerprint;
import org.geysermc.hydraulic.compat.model.Provenance;
import org.geysermc.hydraulic.compat.model.SupportLevel;
import org.geysermc.hydraulic.compat.model.SupportResult;
import org.geysermc.hydraulic.metadata.BlockMapping;
import org.geysermc.hydraulic.metadata.BlockStateRule;
import org.geysermc.hydraulic.metadata.IdentifierMapping;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeDispatchTableTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void compilesDirectRuntimePlansForCurrentBridgeSeams() {
        Identifier block = Identifier.fromNamespaceAndPath("minecraft", "piston");
        Identifier bow = Identifier.fromNamespaceAndPath("minecraft", "bow");
        Identifier entity = Identifier.fromNamespaceAndPath("example", "test_entity");
        Identifier menu = Identifier.fromNamespaceAndPath("example", "test_menu");
        Identifier blockEntity = Identifier.fromNamespaceAndPath("example", "test_block_entity");
        Identifier fluid = Identifier.fromNamespaceAndPath("example", "test_fluid");
        Identifier northBlock = Identifier.fromNamespaceAndPath("example", "north_piston");

        MetadataIndex metadataIndex = new MetadataIndex(
            Map.of(
                block,
                new BlockMapping(block, List.of(new BlockStateRule(
                    Map.of("facing", "north"),
                    northBlock,
                    Map.of("variant", "north"),
                    "example:north_geo",
                    "example:block/north",
                    true,
                    "machine",
                    MappingOwnership.USER,
                    "block.json",
                    1000,
                    0
                )))
            ),
            Map.of(),
            Map.of(),
            Map.of(entity, new IdentifierMapping(entity, Identifier.fromNamespaceAndPath("example", "bedrock_entity"), MappingOwnership.USER, "entity.json", 1000, 0)),
            Map.of(menu, new IdentifierMapping(menu, Identifier.fromNamespaceAndPath("example", "bedrock_menu"), MappingOwnership.USER, "menu.json", 1000, 0)),
            Map.of(
                menu, List.of(new ContentPatch(menu, "menu", Map.of("bedrock.menu.container_type", "generic_9x3"), MappingOwnership.USER, "menu.patch.json", 1000, 0)),
                blockEntity, List.of(new ContentPatch(blockEntity, "block_entity", Map.of("bedrock.block_entity.id", "BedrockChest", "bedrock.block_entity.data.CustomName", "demo"), MappingOwnership.USER, "block_entity.patch.json", 1000, 0))
            ),
            List.of(),
            new MetadataIndex.Summary(2, 0, 0, 0, 1, 1, 2, 0, 0, Map.of("user", 2))
        );

        CompatibilityRegistry registry = new CompatibilityRegistry(
            metadataIndex,
            new MappingResolver(metadataIndex),
            ContentInventory.empty(),
            new CompatibilityReport(
                "2026-09-08T00:00:00Z",
                metadataIndex.summary(),
                List.of(),
                Map.of(
                    "testmod",
                    new CompatibilityProfile(
                        "testmod",
                        fingerprint(),
                        SupportLevel.ADAPTED,
                        CompatibilityStatus.COMPLETE,
                        90,
                        Map.of(),
                        Map.of(),
                        List.of(
                            object("block", block.toString(), Map.of(), supportResults(SupportLevel.ADAPTED, SupportLevel.ADAPTED, SupportLevel.AUTOMATIC)),
                            object("item", bow.toString(), Map.of("behavior_tag", "chargeable_bow"), supportResults(SupportLevel.ADAPTED, SupportLevel.ADAPTED, SupportLevel.AUTOMATIC)),
                            object("entity", entity.toString(), Map.of("behavior_tag", "visual_only_runtime"), supportResults(SupportLevel.ADAPTED, SupportLevel.ADAPTED, SupportLevel.APPROXIMATED)),
                            object("menu", menu.toString(), Map.of(), supportResults(SupportLevel.AUTOMATIC, SupportLevel.AUTOMATIC, SupportLevel.AUTOMATIC)),
                            object("block_entity", blockEntity.toString(), Map.of(), supportResults(SupportLevel.AUTOMATIC, SupportLevel.AUTOMATIC, SupportLevel.AUTOMATIC)),
                            object("fluid", fluid.toString(), Map.of("behavior_tag", "fluid_tank"), supportResults(SupportLevel.AUTOMATIC, SupportLevel.UNSUPPORTED, SupportLevel.UNSUPPORTED))
                        ),
                        List.of(),
                        List.of()
                    )
                )
            )
        );

        var blockPlan = registry.dispatchTable().block(block);
        assertNotNull(blockPlan);

        var blockDefinitions = registry.dispatchTable().blockDefinitions(block);
        assertEquals(2, blockDefinitions.size());

        var resolvedNorthState = registry.dispatchTable().blockState(
            block,
            Blocks.PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH)
        );
        assertNotNull(resolvedNorthState);
        assertEquals("example:north_piston", resolvedNorthState.identifier().toString());
        assertEquals("example:north_geo", resolvedNorthState.metadata().geometryId());
        assertEquals("machine", resolvedNorthState.metadata().behaviorTag());

        var bowPlan = registry.dispatchTable().item(bow);
        assertNotNull(bowPlan);
        assertTrue(bowPlan.supportsAttachablePresentation());
        assertTrue(bowPlan.allowsCustomRegistration());

        var entityPlan = registry.dispatchTable().entity(entity);
        assertNotNull(entityPlan);
        assertEquals("example:bedrock_entity", entityPlan.resolvedIdentifier());
        assertTrue(entityPlan.allowsCustomRegistration());
        assertEquals(SupportLevel.APPROXIMATED, entityPlan.behaviorLevel());

        var menuPlan = registry.dispatchTable().menu(menu);
        assertNotNull(menuPlan);
        assertTrue(menuPlan.requiresMenuBridge());
        assertEquals(ContainerType.GENERIC_9X3, menuPlan.menuFallbackContainerType());

        var blockEntityPlan = registry.dispatchTable().blockEntity(blockEntity);
        assertNotNull(blockEntityPlan);
        assertTrue(blockEntityPlan.requiresBlockEntityRuntime());
        assertNotNull(blockEntityPlan.blockEntityPatchTemplate());
        assertEquals("BedrockChest", blockEntityPlan.blockEntityPatchTemplate().bedrockIdentifier());
        assertEquals(List.of("block_entity_behavior_bridge", "block_entity_data_bridge"), blockEntityPlan.blockEntityRuntimeRequirements());
        assertTrue(blockEntityPlan.requiresRuntimeBridge(RuntimeBridgeKind.BLOCK_ENTITY_DATA));
        assertTrue(blockEntityPlan.requiresRuntimeBridge(RuntimeBridgeKind.BLOCK_ENTITY_BEHAVIOR));

        var fluidPlan = registry.dispatchTable().plan("fluid", fluid.toString());
        assertNotNull(fluidPlan);
        assertEquals(List.of(RuntimeBridgeKind.FLUID_TRANSLATOR, RuntimeBridgeKind.FLUID_RUNTIME), fluidPlan.runtimeBridgeKinds());
        assertEquals(List.of(fluidPlan), registry.dispatchTable().runtimeBridgePlans(RuntimeBridgeKind.FLUID_RUNTIME));

        assertEquals(1, registry.dispatchTable().runtimeBridgePlans(RuntimeBridgeKind.MENU_CONTAINER).size());
        assertEquals(1, registry.dispatchTable().runtimeBridgePlans(RuntimeBridgeKind.BLOCK_ENTITY_DATA).size());

        assertNull(registry.dispatchTable().plan("item", "example:missing"));
        assertEquals(1, registry.dispatchTable().entityPlans("testmod").size());
        assertEquals(1, registry.dispatchTable().menuBridgePlans().size());
        assertEquals(1, registry.dispatchTable().blockEntityBridgePlans().size());
        assertEquals(3, registry.dispatchTable().metrics().blocks().hits());
        assertEquals(1, registry.dispatchTable().metrics().items().hits());
        assertEquals(1, registry.dispatchTable().metrics().items().misses());
        assertEquals(2, registry.dispatchTable().metrics().entities().hits());
        assertEquals(1, registry.dispatchTable().metrics().menus().hits());
        assertEquals(1, registry.dispatchTable().metrics().blockEntities().hits());
    }

    private static CompatibilityObject object(
        String contentType,
        String javaIdentifier,
        Map<String, String> inventoryFacts,
        Map<String, SupportResult> supportResults
    ) {
        return new CompatibilityObject(
            javaIdentifier,
            contentType,
            "testmod",
            inventoryFacts,
            new CapabilityProfile(javaIdentifier, List.of(), List.of()),
            List.of(new AdapterBinding("adapter", AdapterFeature.CUSTOM_ITEM_REGISTRATION, "reason")),
            "menu".equals(contentType)
                ? List.of("runtime.requirement", "container_bridge")
                : "block_entity".equals(contentType)
                    ? List.of("runtime.requirement", "block_entity_data_bridge", "block_entity_behavior_bridge")
                    : "fluid".equals(contentType)
                        ? List.of("runtime.requirement", "fluid_translator", "fluid_runtime_bridge")
                    : List.of("runtime.requirement"),
            supportResults,
            SupportLevel.ADAPTED,
            CompatibilityStatus.COMPLETE,
            90,
            new Confidence(0.95, "high"),
            List.of(new Provenance("analyzer", "generated", "synthetic", false)),
            List.of()
        );
    }

    private static Map<String, SupportResult> supportResults(SupportLevel content, SupportLevel presentation, SupportLevel behavior) {
        return Map.of(
            "content", new SupportResult("content", content, CompatibilityStatus.COMPLETE, 100, List.of("present"), List.of(), List.of()),
            "presentation", new SupportResult("presentation", presentation, CompatibilityStatus.COMPLETE, 100, List.of("present"), List.of(), List.of()),
            "behavior", new SupportResult("behavior", behavior, CompatibilityStatus.COMPLETE, 100, List.of("present"), List.of(), List.of()),
            "interaction", new SupportResult("interaction", SupportLevel.AUTOMATIC, CompatibilityStatus.COMPLETE, 100, List.of("placement"), List.of(), List.of())
        );
    }

    private static ModFingerprint fingerprint() {
        return new ModFingerprint("testmod", "testmod", "1.0.0", "fabric", "26.2", 0, 0, 0, 0, 0, 0, 0, false, false, false, false, false, false, false, false);
    }
}