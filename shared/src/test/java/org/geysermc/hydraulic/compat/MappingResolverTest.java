package org.geysermc.hydraulic.compat;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.hydraulic.metadata.IdentifierMapping;
import org.geysermc.hydraulic.metadata.BlockMapping;
import org.geysermc.hydraulic.metadata.BlockStateRule;
import org.geysermc.hydraulic.metadata.MetadataIndex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MappingResolverTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void resolvesIdentifierMappingsAcrossContentTypes() {
        MetadataIndex index = new MetadataIndex(
            Map.of(),
            Map.of(
                Identifier.fromNamespaceAndPath("example", "test_item"),
                new IdentifierMapping(
                    Identifier.fromNamespaceAndPath("example", "test_item"),
                    Identifier.fromNamespaceAndPath("example", "bedrock_item"),
                    MappingOwnership.USER,
                    "test.json",
                    MappingOwnership.USER.priority(),
                    0
                )
            ),
            Map.of(
                Identifier.fromNamespaceAndPath("example", "test_recipe"),
                new IdentifierMapping(
                    Identifier.fromNamespaceAndPath("example", "test_recipe"),
                    Identifier.fromNamespaceAndPath("example", "bedrock_recipe"),
                    MappingOwnership.USER,
                    "test.json",
                    MappingOwnership.USER.priority(),
                    0
                )
            ),
            Map.of(
                Identifier.fromNamespaceAndPath("example", "test_entity"),
                new IdentifierMapping(
                    Identifier.fromNamespaceAndPath("example", "test_entity"),
                    Identifier.fromNamespaceAndPath("example", "bedrock_entity"),
                    MappingOwnership.USER,
                    "test.json",
                    MappingOwnership.USER.priority(),
                    0
                )
            ),
            Map.of(
                Identifier.fromNamespaceAndPath("example", "test_menu"),
                new IdentifierMapping(
                    Identifier.fromNamespaceAndPath("example", "test_menu"),
                    Identifier.fromNamespaceAndPath("example", "bedrock_menu"),
                    MappingOwnership.USER,
                    "test.json",
                    MappingOwnership.USER.priority(),
                    0
                )
            ),
            Map.of(),
            java.util.List.of(),
            new MetadataIndex.Summary(1, 0, 1, 1, 1, 1, 0, 0, 0, Map.of("user", 1))
        );

        MappingResolver resolver = new MappingResolver(index);

        assertEquals("example:bedrock_item", resolver.resolveItemIdentifier(Identifier.fromNamespaceAndPath("example", "test_item")).identifier().toString());
        assertEquals("example:bedrock_recipe", resolver.resolveRecipeIdentifier(Identifier.fromNamespaceAndPath("example", "test_recipe")).identifier().toString());
        assertEquals("example:bedrock_entity", resolver.resolveEntityIdentifier(Identifier.fromNamespaceAndPath("example", "test_entity")).identifier().toString());
        assertEquals("example:bedrock_menu", resolver.resolveMenuIdentifier(Identifier.fromNamespaceAndPath("example", "test_menu")).identifier().toString());
        assertFalse(resolver.resolveItemIdentifier(Identifier.fromNamespaceAndPath("example", "missing_item")).overridden());
    }

    @Test
    void exposesCompactResolvedBlockMetadata() {
        BlockStateRule northRule = new BlockStateRule(
            Map.of("facing", "north"),
            Identifier.fromNamespaceAndPath("example", "north_block"),
            Map.of("variant", "north"),
            "example:north_geo",
            "example:block/north",
            true,
            "machine",
            MappingOwnership.USER,
            "test.json",
            MappingOwnership.USER.priority(),
            0
        );
        MetadataIndex index = new MetadataIndex(
            Map.of(
                Identifier.fromNamespaceAndPath("example", "test_block"),
                new BlockMapping(Identifier.fromNamespaceAndPath("example", "test_block"), java.util.List.of(northRule))
            ),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            java.util.List.of(),
            new MetadataIndex.Summary(1, 1, 0, 0, 0, 0, 0, 1, 0, Map.of("user", 1))
        );

        MappingResolver resolver = new MappingResolver(index);
        MappingResolver.ResolvedBlockState resolved = resolver.resolveBlockState(
            Identifier.fromNamespaceAndPath("example", "test_block"),
            Blocks.PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH)
        );

        assertEquals("example:north_block", resolved.identifier().toString());
        assertEquals("example:north_geo", resolved.metadata().geometryId());
        assertEquals("example:block/north", resolved.metadata().materialId());
        assertEquals("north", resolved.metadata().bedrockState().get("variant"));
        assertEquals("machine", resolved.metadata().behaviorTag());
        assertTrue(resolved.metadata().behaviorRequired());
    }

    @Test
    void resolvesCompactRuntimePatchTemplates() {
        org.geysermc.hydraulic.compat.mapping.ContentPatch menuPatch = new org.geysermc.hydraulic.compat.mapping.ContentPatch(
            Identifier.fromNamespaceAndPath("example", "test_menu"),
            "menu",
            Map.of("bedrock.menu.container_type", "generic_9x3"),
            MappingOwnership.USER,
            "test.json",
            MappingOwnership.USER.priority(),
            0
        );
        org.geysermc.hydraulic.compat.mapping.ContentPatch blockEntityPatch = new org.geysermc.hydraulic.compat.mapping.ContentPatch(
            Identifier.fromNamespaceAndPath("example", "test_block_entity"),
            "block_entity",
            Map.of(
                "bedrock.block_entity.id", "Barrel",
                "bedrock.block_entity.data.TransferCooldown", "8"
            ),
            MappingOwnership.USER,
            "test.json",
            MappingOwnership.USER.priority(),
            1
        );

        MetadataIndex index = new MetadataIndex(
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(
                menuPatch.target(), List.of(menuPatch),
                blockEntityPatch.target(), List.of(blockEntityPatch)
            ),
            java.util.List.of(),
            MetadataIndex.Summary.empty()
        );

        MappingResolver resolver = new MappingResolver(index);
        var menuTemplate = resolver.menuPatchTemplate(menuPatch.target());
        var blockEntityTemplate = resolver.blockEntityPatchTemplate(blockEntityPatch.target());

        assertNotNull(menuTemplate);
        assertNotNull(blockEntityTemplate);
        assertEquals(ContainerType.GENERIC_9X3, menuTemplate.fallbackContainerType());
        assertEquals("Barrel", blockEntityTemplate.bedrockIdentifier());
        assertEquals(1, blockEntityTemplate.mutations().size());
        assertNull(resolver.menuPatchTemplate(Identifier.fromNamespaceAndPath("example", "missing_menu")));
        assertNull(resolver.blockEntityPatchTemplate(Identifier.fromNamespaceAndPath("example", "missing_block_entity")));
    }
}